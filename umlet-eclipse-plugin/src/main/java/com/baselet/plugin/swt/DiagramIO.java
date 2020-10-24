package com.baselet.plugin.swt;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.config.Config;
import com.baselet.control.enums.ElementId;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.gui.BaseGUI;
import com.baselet.gui.CurrentGui;

public class DiagramIO {
	private static final Logger log = LoggerFactory.getLogger(DiagramIO.class);

	public static void readFromFile(File file, Diagram diagram, IElementFactory factory) {
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
			XMLInputHandler xmlhandler = new XMLInputHandler(diagram, factory);
			parser.parse(input, xmlhandler);
			input.close();
		} catch (Exception e) {
			log.error("Cannot open the file: " + file.getAbsolutePath(), e);
		}
	}

	public static void readFromString(String content, Diagram diagram, IElementFactory factory) {
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
			XMLInputHandler xmlhandler = new XMLInputHandler(diagram, factory);
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

		public XMLInputHandler(Diagram diagram, IElementFactory factory) {
			this.diagram = diagram;
			this.factory = factory;
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

			if (elementname.equals("help_text")) {
				// TODO@fab needed? (not in gwt variant)
				// diagram.setHelpText(elementtext);
				// diagram.getFontHandler().setDiagramDefaultFontSize(HelpPanelChanged.getFontsize(elementtext));
				// diagram.getFontHandler().setDiagramDefaultFontFamily(HelpPanelChanged.getFontfamily(elementtext));
				BaseGUI gui = CurrentGui.getInstance().getGui();
				if (gui != null && gui.getPropertyPane() != null) { // issue 244: in batchmode, a file can have a help_text but gui will be null
					gui.getPropertyPane().switchToNonElement(elementText);
				}
			}
			// TODO@fab needed? (not in gwt variant)
			// else if (elementname.equals("zoom_level")) {
			// if (diagram != null) {
			// diagram.setGridSize(Integer.parseInt(elementtext));
			// }
			// }
			else if (elementname.equals("element")) {
				// TODO@fab check: do not support old elements that have no id
				try {
					GridElement e = factory.create(ElementId.valueOf(id), new Rectangle(x, y, w, h), panelAttributes, additionalAttributes, diagram);
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

}
