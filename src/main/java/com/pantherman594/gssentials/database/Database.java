/*
 * BungeeEssentials: Full customization of a few necessary features for your server!
 * Copyright (C) 2016 David Shen (PantherMan594)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pantherman594.gssentials.database;

import com.pantherman594.gssentials.BungeeEssentials;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by david on 7/30.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Database {
    String tableName;
    boolean isNewMySql;

    private final String primary;
    private Connection connection;
    private final boolean mysql;
    private String url;
    private String username;
    private String password;
    private int uses = 0;

    public Database(String tableName, String setupSql, String primary) {
        this.tableName = tableName;
        this.primary = primary;
        mysql = false;
        load(setupSql, primary);
    }

    public Database(String tableName, String setupSql, String primary, String url, String username, String password) {
        this.tableName = tableName;
        this.primary = primary;
        mysql = true;
        this.url = url;
        this.username = username;
        this.password = password;
        load(setupSql, primary);
    }

    static Set<String> setFromString(String input) {
        Set<String> set = new HashSet<>();
        if (input != null && !input.equals("")) {
            Collections.addAll(set, input.split(";"));
        }
        return set;
    }

    public String getTableName() {
        return tableName;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    Connection getSQLConnection() {
        File dbFile = new File(BungeeEssentials.getInstance().getDataFolder(), tableName + ".db");
        if (!mysql && !dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (uses > 250) {
                connection.close();
            }

            if (connection != null && !connection.isClosed()) {
                uses++;
                return connection;
            }

            if (!mysql) {
                File sqliteLib = new File(BungeeEssentials.getInstance().getLibDir(), "sqlite-jdbc-3.8.11.2.jar");

                if (!sqliteLib.exists()) {
                    BungeeEssentials.getInstance().getLogger().log(Level.INFO, "Downloading SQLite JDBC library...");
                    String dlLink = "https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.8.11.2.jar";
                    URLConnection con;
                    try {
                        URL url = new URL(dlLink);
                        con = url.openConnection();
                    } catch (IOException e) {
                        BungeeEssentials.getInstance().getLogger().log(Level.SEVERE, "Invalid SQLite download link. Please contact plugin author.");
                        return null;
                    }

                    try (
                            InputStream in = con.getInputStream();
                            FileOutputStream out = new FileOutputStream(sqliteLib)
                    ) {
                        byte[] buffer = new byte[1024];
                        int size;
                        while ((size = in.read(buffer)) != -1) {
                            out.write(buffer, 0, size);
                        }
                    } catch (IOException e) {
                        BungeeEssentials.getInstance().getLogger().log(Level.WARNING, "Failed to download update, please download it manually from https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.8.11.2.jar and put it in the /plugins/BungeeEssentials/lib folder.");
                        BungeeEssentials.getInstance().getLogger().log(Level.WARNING, "Error message: ");
                        e.printStackTrace();
                        return null;
                    }
                }

                URLClassLoader loader = new URLClassLoader(new URL[]{sqliteLib.toURI().toURL()});
                Method m = DriverManager.class.getDeclaredMethod("getConnection", String.class, Properties.class, Class.class);
                m.setAccessible(true);

                connection = (Connection) m.invoke(null, "jdbc:sqlite:" + dbFile.getPath(), new Properties(), Class.forName("org.sqlite.JDBC", true, loader));

                uses = 0;
                return connection;
            } else {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://" + url, username, password);

                DatabaseMetaData conMeta = connection.getMetaData();
                ResultSet findTable = conMeta.getTables(null, null, tableName, null);
                isNewMySql = !findTable.next();

                uses = 0;
                return connection;
            }

        } catch (ClassNotFoundException e) {
            BungeeEssentials.getInstance().getLogger().log(Level.SEVERE, "You are missing necessary libraries. If using SQLite, download it from https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.8.11.2.jar and put it in the /plugins/BungeeEssentials/lib folder.");
        } catch (Exception e) {
            BungeeEssentials.getInstance().getLogger().log(Level.SEVERE, "Exception on SQL initialize", e);
        }

        return null;
    }

    public abstract boolean createDataNotExist(String keyVal);

    public List<Object> listAllData(String label) {
        List<Object> datas = new ArrayList<>();

        Connection conn = getSQLConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName + ";");
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                datas.add(rs.getObject(label));
            }

            if (datas.size() > 0) {
                return datas;
            }

        } catch (SQLException e) {
            BungeeEssentials.getInstance().getLogger().log(Level.SEVERE, "Couldn't execute SQLite statement: ", e);
        }
        return null;
    }

    public List<Object> getDataMultiple(String key, String keyVal, String label) {
        //TODO: Cache queries into memory
        List<Object> datas = new ArrayList<>();

        Connection conn = getSQLConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE " + key + " = ?;")
        ) {
            ps.setObject(1, keyVal);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                if (rs.getString(key).equals(keyVal)) {
                    datas.add(rs.getObject(label));
                }
            }

            if (datas.size() > 0) {
                return datas;
            }

        } catch (SQLException e) {
            BungeeEssentials.getInstance().getLogger().log(Level.SEVERE, "Couldn't execute SQLite statement: ", e);
        }
        return null;
    }

    public Object getData(String key, String keyVal, String label) {
        List<Object> datas = getDataMultiple(key, keyVal, label);
        if (datas != null) {
            return datas.get(0);
        }
        return null;
    }

    public void setData(String key, String keyVal, String label, Object labelVal) {
        if (key.equals(primary) && !createDataNotExist(keyVal)) {
            return;
        }

        Connection conn = getSQLConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("UPDATE " + tableName + " SET " + label + " = ? WHERE " + key + " = ?;")
        ) {
            ps.setObject(1, labelVal);
            ps.setObject(2, keyVal);
            ps.executeUpdate();
        } catch (SQLException e) {
            BungeeEssentials.getInstance().getLogger().log(Level.SEVERE, "Couldn't execute SQLite statement: ", e);
        }
    }

    void setValues(PreparedStatement ps, Object... values) throws SQLException {
        setValues(1, ps, values);
    }

    void setValues(int start, PreparedStatement ps, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            ps.setObject(i + start, values[i]);
        }
    }

    private void load(String setupSql, String primary) {
        connection = getSQLConnection();

        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " " + setupSql + ",PRIMARY KEY (`" + primary + "`));");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void execute(String sql) {
        Connection conn = getSQLConnection();
        try (Statement s = conn.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
