package com.baselet.plugin.swt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.config.Config;
import com.baselet.control.constants.Constants;
import com.baselet.control.constants.SharedConstants;
import com.baselet.control.enums.ElementId;
import com.baselet.control.enums.Program;
import com.baselet.element.NewGridElement;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.old.custom.CustomElement;

public class DiagramIO {
	private static final Logger log = LoggerFactory.getLogger(DiagramIO.class);

	public static void readFromFile(File file, Diagram diagram, IElementFactory factory, StringBuilder helpText) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			if (Config.getInstance().isSecureXmlProcessing()) {
				// use secure xml processing (see https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXP_DocumentBuilderFactory.2C_SAXParserFactory_and_DOM4J)
				spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
				spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			}
			SAXParser parser = spf.newSAXParser();
			FileInputStream input = new FileInputStream(file);
			XMLInputHandler xmlhandler = new XMLInputHandler(diagram, factory, helpText);
			parser.parse(input, xmlhandler);
			input.close();
		} catch (Exception e) {
			log.error("Cannot open the file: " + file.getAbsolutePath(), e);
		}
	}

	public static void readFromString(String content, Diagram diagram, IElementFactory factory, StringBuilder helpText) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			if (Config.getInstance().isSecureXmlProcessing()) {
				// use secure xml processing (see https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXP_DocumentBuilderFactory.2C_SAXParserFactory_and_DOM4J)
				spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
				spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			}
			SAXParser parser = spf.newSAXParser();
			XMLInputHandler xmlhandler = new XMLInputHandler(diagram, factory, helpText);
			StringReader input = new StringReader(content);
			parser.parse(new InputSource(input), xmlhandler);
			input.close();
		} catch (Exception e) {
			log.error("Cannot parse content", e);
		}
	}

	private static class XMLInputHandler extends DefaultHandler {
		private final Diagram diagram;
		private final IElementFactory factory;

		// parse state
		private String elementText;
		private int x;
		private int y;
		private int w;
		private int h;
		private String panelAttributes;
		private String additionalAttributes;
		private String id;
		private float scaleFactor = 1.0f;
		private final StringBuilder helpTextReceiver;
		private final Optional<String> helpText = Optional.empty();

		public XMLInputHandler(Diagram diagram, IElementFactory factory, StringBuilder helpText) {
			this.diagram = diagram;
			this.factory = factory;
			helpTextReceiver = helpText;
		}

		@Override
		public void endDocument() throws SAXException {
			if (helpTextReceiver != null) {
				helpTextReceiver.append(helpText.orElse(Constants.getDefaultHelptext()));
			}
			super.endDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			elementText = "";
			if (qName.equals("element")) {
				panelAttributes = "";
				additionalAttributes = "";
				id = null;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			String elementname = qName; // [UB]: we are not name-space aware, so use the qualified name

			if (elementname.equals("help_text") && helpTextReceiver != null) {
				Optional.of(elementText);
				// TODO@fab needed? (not in gwt variant)
				// diagram.getFontHandler().setDiagramDefaultFontSize(HelpPanelChanged.getFontsize(elementtext));
				// diagram.getFontHandler().setDiagramDefaultFontFamily(HelpPanelChanged.getFontfamily(elementtext));
			}
			// TODO@fab needed? (not in gwt variant)
			else if (elementname.equals("zoom_level")) {
				if (diagram != null) {
					int gridSize = Integer.parseInt(elementText);
					scaleFactor = (float) SharedConstants.DEFAULT_GRID_SIZE / gridSize;
				}
			}
			else if (elementname.equals("element")) {
				// TODO@fab check: do not support old elements that have no id
				try {
					Rectangle rect = new Rectangle((int) (x * scaleFactor), (int) (y * scaleFactor), (int) (w * scaleFactor), (int) (h * scaleFactor));
					GridElement e = factory.create(ElementId.valueOf(id), rect, panelAttributes, additionalAttributes, diagram);
					diagram.getGridElements().add(e);
				} catch (Exception e) {
					log.error("Cannot instantiate element with id: " + id, e);
				}
				id = null;
			}
			else if (elementname.equals("id")) { // new elements have an id
				id = elementText;
			}
			else if (elementname.equals("x")) {
				Integer i = Integer.valueOf(elementText);
				x = i.intValue();
			}
			else if (elementname.equals("y")) {
				Integer i = Integer.valueOf(elementText);
				y = i.intValue();
			}
			else if (elementname.equals("w")) {
				Integer i = Integer.valueOf(elementText);
				w = i.intValue();
			}
			else if (elementname.equals("h")) {
				Integer i = Integer.valueOf(elementText);
				h = i.intValue();
			}
			else if (elementname.equals("panel_attributes")) {
				panelAttributes = elementText;
			}
			else if (elementname.equals("additional_attributes")) {
				additionalAttributes = elementText;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			elementText += new String(ch).substring(start, start + length);
		}
	}

	public static void writeToFile(Diagram diagram, int zoomLevel, File file, String helpText) {
		String content = writeToString(diagram, zoomLevel, helpText);
		try {
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(content);
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String writeToString(Diagram diagram, int zoomLevel, String helpText) {
		DocumentBuilder db = null;
		String returnString = null;

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			Element root = doc.createElement("diagram");
			root.setAttribute("program", Program.getInstance().getProgramName().toLowerCase());
			root.setAttribute("version", String.valueOf(Program.getInstance().getVersion()));
			doc.appendChild(root);

			// save helptext
			if (helpText != null && !helpText.equals(Constants.getDefaultHelptext())) {
				Element help = doc.createElement("help_text");
				help.appendChild(doc.createTextNode(helpText));
				root.appendChild(help);
			}

			// save zoom
			Element zoom = doc.createElement("zoom_level");
			zoom.appendChild(doc.createTextNode(String.valueOf(zoomLevel)));
			root.appendChild(zoom);

			createXMLOutputDoc(doc, diagram.getGridElements(), root);

			// output the stuff...
			DOMSource source = new DOMSource(doc);
			StringWriter stringWriter = new StringWriter();
			StreamResult result = new StreamResult(stringWriter);

			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			transformer.transform(source, result);

			returnString = stringWriter.toString();
		} catch (Exception e) {
			log.error("Error saving XML.", e);
		}

		return returnString;
	}

	private static void createXMLOutputDoc(Document doc, Collection<GridElement> elements, Element current) {
		for (GridElement e : elements) {
			appendRecursively(doc, current, e);
		}
	}

	private static void appendRecursively(Document doc, Element parentXmlElement, GridElement e) {
		parentXmlElement.appendChild(createXmlElementForGridElement(doc, e));
	}

	private static Element createXmlElementForGridElement(Document doc, GridElement e) {
		// insert normal entity element
		java.lang.Class<? extends GridElement> c = e.getClass();
		String sElType = c.getName();
		String sElPanelAttributes = e.getPanelAttributes();
		String sElAdditionalAttributes = e.getAdditionalAttributes();

		Element el = doc.createElement("element");

		if (e instanceof NewGridElement) {
			Element elType = doc.createElement("id");
			elType.appendChild(doc.createTextNode(((NewGridElement) e).getId().toString()));
			el.appendChild(elType);
		}
		else { // OldGridElement
			Element elType = doc.createElement("type");
			elType.appendChild(doc.createTextNode(sElType));
			el.appendChild(elType);
		}

		Element elCoor = doc.createElement("coordinates");
		el.appendChild(elCoor);

		Element elX = doc.createElement("x");
		elX.appendChild(doc.createTextNode("" + e.getRectangle().x));
		elCoor.appendChild(elX);

		Element elY = doc.createElement("y");
		elY.appendChild(doc.createTextNode("" + e.getRectangle().y));
		elCoor.appendChild(elY);

		Element elW = doc.createElement("w");
		elW.appendChild(doc.createTextNode("" + e.getRectangle().width));
		elCoor.appendChild(elW);

		Element elH = doc.createElement("h");
		elH.appendChild(doc.createTextNode("" + e.getRectangle().height));
		elCoor.appendChild(elH);

		Element elPA = doc.createElement("panel_attributes");
		elPA.appendChild(doc.createTextNode(sElPanelAttributes));
		el.appendChild(elPA);

		Element elAA = doc.createElement("additional_attributes");
		elAA.appendChild(doc.createTextNode(sElAdditionalAttributes));
		el.appendChild(elAA);

		if (e instanceof CustomElement) {
			Element elCO = doc.createElement("custom_code");
			elCO.appendChild(doc.createTextNode(((CustomElement) e).getCode()));
			el.appendChild(elCO);
		}
		return el;
	}
}
