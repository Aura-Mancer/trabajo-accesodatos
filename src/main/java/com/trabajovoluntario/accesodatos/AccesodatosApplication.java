package com.trabajovoluntario.accesodatos;

import java.io.File;
import java.sql.Timestamp;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SpringBootApplication
public class AccesodatosApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccesodatosApplication.class, args);

		//Instanciamos las fabricas
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		XPath xPath = XPathFactory.newInstance().newXPath();

		// Inicializamos la conexión con la base de datos
		ConexionH2 bd = new ConexionH2();

		try {
			// Preparamos la tabla de la base de datos relacional, borrando la actual si existe.
			bd.sentencia("DROP TABLE IF EXISTS CONTRATOS;CREATE TABLE CONTRATOS (NIF VARCHAR(9),ADJUDICATARIO VARCHAR(100),OBJETO_GENERICO VARCHAR(100),OBJETO VARCHAR(100),FECHA_DE_ADJUDICACION TIMESTAMP,IMPORTE DOUBLE,PROVEEDORES_CONSULTADOS VARCHAR(100),TIPO_DE_CONTRATO VARCHAR(100));");
			// Obtenemos el archivo y la referencia al documento XML
			File archivo = new File("src/main/resources/contratos-adjudicados-oct-23.xml");
			Document doc = builder.parse(archivo);
			doc.getDocumentElement().normalize();

			// Obtenemos todos una lista de todos los nodos de fila
			NodeList filas = (NodeList) xPath.compile("//Row").evaluate(doc, XPathConstants.NODESET);

			// Iteramos sobre cada fila, añadiendolas a la base de datos relacional
			for (int i = 2; i < filas.getLength(); i++) {
				// Hay filas que están corruptas y les faltan o sobran datos, solo procesamos las filas que contengan todos los datos correctamente
				if(xPath.compile("count(//Row[" + i + "]//Cell/Data)").evaluate(doc, XPathConstants.STRING).toString().equals("8")){
					// Eliminamos cualquier guion o espacio que exista en el NIF
					String nif = xPath.compile("//Row[" + i + "]//Cell[1]/Data/text()").evaluate(doc, XPathConstants.STRING).toString().replace(" ","").replace("-","");
					String adjudicatario = xPath.compile("//Row[" + i + "]//Cell[2]/Data/text()").evaluate(doc, XPathConstants.STRING).toString();
					String objeto_generico = xPath.compile("//Row[" + i + "]//Cell[3]/Data/text()").evaluate(doc, XPathConstants.STRING).toString();
					String objeto = xPath.compile("//Row[" + i + "]//Cell[4]/Data/text()").evaluate(doc, XPathConstants.STRING).toString();
					String fecha_de_adjudicacion = xPath.compile("//Row[" + i + "]//Cell[5]/Data/text()").evaluate(doc, XPathConstants.STRING).toString();
					if(!fecha_de_adjudicacion.contains("-")){ //Si la ficha esta en formato DD.MM.AAAA, se transforma a el formato TimeStamp reemplazando caracteres
						fecha_de_adjudicacion = fecha_de_adjudicacion.replaceAll("([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{1,4})","$3-$2-$1T00:00:00.000");
					}
					if(!fecha_de_adjudicacion.contains("T")){ //Si la fecha esta en formato de excel, se transforma al Epoch de Unix y despues a una TimeStamp.
						fecha_de_adjudicacion = new Timestamp(Long.valueOf(fecha_de_adjudicacion)*24*60*60*1000-2209161600000L).toString();
					}

					// Se elimina el separador de los miles y se convierte el separador "," por ". para que lo acepte SQL"
					String importe = xPath.compile("//Row[" + i + "]//Cell[6]/Data/text()").evaluate(doc, XPathConstants.STRING).toString().replace(".","").replace(',', '.').replace(" €", "");

					// Si un proveedor esta sin especificar con "-" o "_" se cambia por NULL
					String proveedores_consultados = xPath.compile("//Row[" + i + "]//Cell[7]/Data/text()").evaluate(doc, XPathConstants.STRING).toString().replace("_", "NULL").replace("-", "NULL");
					String tipo_de_contrato = xPath.compile("//Row[" + i + "]//Cell[8]/Data/text()").evaluate(doc, XPathConstants.STRING).toString();

					// Se insertan los datos obtenidos en la base de datos
					bd.actualizacion("INSERT INTO CONTRATOS VALUES('" + nif + "','" + adjudicatario + "','" + objeto_generico + "','" + objeto + "','" + fecha_de_adjudicacion + "','" + importe + "','" + proveedores_consultados + "','" + tipo_de_contrato + "')");

					// Se elimina el tipo de contrato de la fila en el documento xml
					Node tipo_de_contrato_a_eliminar = (Node) xPath.compile("//Row[" + i + "]//Cell[8]").evaluate(doc, XPathConstants.NODE);
					tipo_de_contrato_a_eliminar.getParentNode().removeChild(tipo_de_contrato_a_eliminar);
				}
			}
			// Cerramos la conexión a la base de datos
			bd.cerrar();

			// Se elimina la cabecera de tipo de contrato del documento xml
			Node cabezera_tipo_contrato = (Node) xPath.compile("//Row[1]//Cell[8]").evaluate(doc, XPathConstants.NODE);
			cabezera_tipo_contrato.getParentNode().removeChild(cabezera_tipo_contrato);

			// Se guardan los cambios en una copia del archivo xml
			StreamResult result = new StreamResult(new File("src/main/resources/contratos-adjudicados-oct-23-salida.xml"));
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), result);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

