package com.trabajovoluntario.accesodatos;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;


public class ConexionH2 {
    // Se configura la conexion a la base de datos
    private static final String driverH2 = "org.springframework.boot.jdbc.DatabaseDriver";
    private static final String urlConexion = "jdbc:h2:file:./base_de_datos";
    private static final String usuario = "user_db";
    private static final String password = "password_db";
    private Connection dbConnection = null;

    public ConexionH2(){
        try {
            Class.forName(driverH2); // Se comprueba que existe el JBDC driver para H2
            dbConnection = DriverManager.getConnection(urlConexion, usuario, password);

        } catch (SQLException e) {
            System.out.println("Error SQL" + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("El driver JDBC no existe: " + e.getMessage());
        }
    }

    public int actualizacion(String sentencia) throws SQLException{
        return dbConnection.createStatement().executeUpdate(sentencia);
    }

    public boolean sentencia(String sentencia) throws SQLException{
        return dbConnection.createStatement().execute(sentencia);
    }

    public void cerrar(){
        try {
            dbConnection.close();
        } catch (Exception e) {
            System.out.println("Error al cerrar la base de datos " + e.getMessage());
        }
    }
}

