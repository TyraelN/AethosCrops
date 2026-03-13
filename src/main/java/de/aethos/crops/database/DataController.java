package de.aethos.crops.database;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.crop.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class DataController {
    private final String url;

    private final String user;

    private final String password;

    public DataController(AethosCrops plugin) {
        FileConfiguration config = plugin.getConfig();
        url = config.getString("mysql.url");
        user = config.getString("mysql.user");
        password = config.getString("mysql.pwd");
    }

    public void start() {
        createPlantTable();
        loadCrops();
    }

    public void createPlantTable() {
        write("create table if not exists crops (CordX int, CordY int, CordZ int, World varchar(30), Croptype varchar(10), Krankheit varchar(15), Zeitpunkt long, " + Gen.KAEFER + " int," + Gen.PILZ + " int," + Gen.UNKRAUT + " int," + Gen.WACHSTUM + " int," + Gen.MENGE + " int," + Gen.AUSDAUER + " int, primary key (CordX, CordY, CordZ, world));");
    }

    public boolean write(String string) {
        try {
            Connection con = DriverManager.getConnection(Objects.requireNonNull(url), user, password);
            Statement statement = con.createStatement();
            statement.execute(string);
            con.close();
            statement.close();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadCrops() {
        String anfrage = "select * from crops;";
        try {
            ResultSet result = search(anfrage);
            while (result.next()) {
                loadCrop(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultSet search(String string) throws SQLException {
        Connection con = DriverManager.getConnection(Objects.requireNonNull(url), user, password);
        Statement statement = con.createStatement();
        statement.executeQuery(string);
        return statement.getResultSet();
    }

    public void loadCrop(ResultSet result) throws SQLException {
        int x = result.getInt(1);
        int y = result.getInt(2);
        int z = result.getInt(3);
        String world = result.getString(4);
        Location loc = new Location(Bukkit.getWorld(world), x, y, z);
        CropType cropType = CropType.valueOf(result.getString(5));
        Krankheit krankheit = Krankheit.valueOf(result.getString(6));
        long zeitpunkt = result.getLong(7);
        Map<Gen, Integer> map = new EnumMap<>(Gen.class);
        for (Gen gen : Gen.values()) {
            map.put(gen, result.getInt(gen.toString()));
        }
        CropFactory.getCrop(loc, cropType, krankheit, map, zeitpunkt);
    }

    public void stop() {
        deleteCrops();
        createPlantTable();
        saveCrops();
    }

    public void saveCrops() {
        for (Crop crop : CropManager.getCrops().values()) {
            if (crop.isValid()) {
                saveCrop(crop);
            }

        }
    }

    public void saveCrop(Crop crop) {
        if (crop.isTooOld()) {
            return;
        }
        int cordX = crop.getLoc().getBlockX();
        int cordY = crop.getLoc().getBlockY();
        int cordZ = crop.getLoc().getBlockZ();
        String world = crop.getLoc().getWorld().getName();
        CropType cropType = crop.getType();
        String krankheit = String.valueOf(crop.getKrankheit());
        int genk = crop.getGen(Gen.KAEFER);
        int genp = crop.getGen(Gen.PILZ);
        int genu = crop.getGen(Gen.UNKRAUT);
        int genw = crop.getGen(Gen.WACHSTUM);
        int genm = crop.getGen(Gen.MENGE);
        int gena = crop.getGen(Gen.AUSDAUER);
        long zeitpunkt = crop.getZeitpunkt();
        write("insert into crops (CordX, CordY, CordZ, World, CropType, Krankheit, Zeitpunkt,"
                + Gen.KAEFER + "," + Gen.PILZ + "," + Gen.UNKRAUT + "," + Gen.WACHSTUM + "," + Gen.MENGE + "," + Gen.AUSDAUER + ") "
                + "values (" + cordX + "," + cordY + "," + cordZ + ",'" + world + "','" + cropType + "','" + krankheit + "','" + zeitpunkt + "','"
                + genk + "','" + genp + "','" + genu + "','" + genw + "','" + genm + "','" + gena + "');");
    }

    public void deleteCrops() {
        write("drop table if exists crops;");
    }
}
