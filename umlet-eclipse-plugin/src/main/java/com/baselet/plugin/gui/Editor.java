package com.baselet.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.control.enums.Program;
import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.DrawPanel;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.old.custom.CustomElementHandler;
import com.baselet.gui.CurrentGui;
import com.baselet.gui.listener.UmletWindowFocusListener;
import com.baselet.gui.pane.OwnSyntaxPane;
import com.baselet.plugin.swt.DiagramIO;
import com.baselet.plugin.swt.IElementFactory;
import com.baselet.plugin.swt.SWTComponent;
import com.baselet.plugin.swt.SWTDiagramHandler;
import com.baselet.plugin.swt.SWTElementFactory;

public class Editor extends EditorPart {

	private static final Logger log = LoggerFactory.getLogger(Editor.class);

	private DiagramHandler handler;
	private Panel embeddedPanel;

	private final EclipseGUIBuilder guiComponents = new EclipseGUIBuilder();

	private final UUID uuid = UUID.randomUUID();

	@Override
	public void doSave(IProgressMonitor monitor) {
		handler.doSave();
		monitor.done();
	}

	@Override
	public void doSaveAs() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handler.doSaveAs(Program.getInstance().getExtension());
			}
		});
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	File diagramFile;

	private Frame mainFrame;

	private Composite mainComposite;

	private SashForm mainForm;

	private SashForm sidebarForm;

	private Canvas paletteCanvas;

	private TextViewer propertiesTextViewer;

	private SWTOwnPropertyPane swtOwnPropertyPane;

	// TEST ONLY
	private final String paletteContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" +
											"<diagram program=\"umlet\" version=\"14.2.0\">\r\n" +
											"  <zoom_level>8</zoom_level>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLClass</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>16</x>\r\n" +
											"      <y>48</y>\r\n" +
											"      <w>168</w>\r\n" +
											"      <h>152</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>&lt;&lt;Stereotype&gt;&gt;\r\n" +
											"Package::FatClass\r\n" +
											"{Some Properties}\r\n" +
											"--\r\n" +
											"-id: Long\r\n" +
											"_-ClassAttribute: Long_\r\n" +
											"--\r\n" +
											"#Operation(i: int): int\r\n" +
											"/+AbstractOperation()/\r\n" +
											"--\r\n" +
											"Responsibilities\r\n" +
											"-- Resp1\r\n" +
											"*-- Resp2*</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLClass</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>104</x>\r\n" +
											"      <y>16</y>\r\n" +
											"      <w>80</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>/AbstractClass/\r\n" +
											"</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLClass</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>16</x>\r\n" +
											"      <y>240</y>\r\n" +
											"      <w>168</w>\r\n" +
											"      <h>56</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>_object: Class_\r\n" +
											"--\r\n" +
											"id: Long=\"36548\"\r\n" +
											"[waiting for message]</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLClass</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>288</x>\r\n" +
											"      <y>56</y>\r\n" +
											"      <w>48</w>\r\n" +
											"      <h>16</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Rose\r\n" +
											"bg=red\r\n" +
											"</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLUseCase</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>16</x>\r\n" +
											"      <y>304</y>\r\n" +
											"      <w>96</w>\r\n" +
											"      <h>32</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Use case 1</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLUseCase</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>104</x>\r\n" +
											"      <y>328</y>\r\n" +
											"      <w>96</w>\r\n" +
											"      <h>32</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>*Use case 3*</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLUseCase</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>16</x>\r\n" +
											"      <y>376</y>\r\n" +
											"      <w>96</w>\r\n" +
											"      <h>32</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Use case 2\r\n" +
											"bg=blue</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLUseCase</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>16</x>\r\n" +
											"      <y>448</y>\r\n" +
											"      <w>96</w>\r\n" +
											"      <h>32</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=.\r\n" +
											"Collaboration\r\n" +
											"fg=red\r\n" +
											"bg=yellow</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLActor</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>136</x>\r\n" +
											"      <y>408</y>\r\n" +
											"      <w>48</w>\r\n" +
											"      <h>88</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Actor</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLNote</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>216</x>\r\n" +
											"      <y>304</y>\r\n" +
											"      <w>112</w>\r\n" +
											"      <h>56</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Note..\r\n" +
											"bg=blue</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLPackage</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>216</x>\r\n" +
											"      <y>376</y>\r\n" +
											"      <w>112</w>\r\n" +
											"      <h>56</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>EmptyPackage\r\n" +
											"--\r\n" +
											"bg=orange</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLPackage</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>216</x>\r\n" +
											"      <y>448</y>\r\n" +
											"      <w>112</w>\r\n" +
											"      <h>56</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Package 1\r\n" +
											"--\r\n" +
											"-Content 1\r\n" +
											"+Content 2\r\n" +
											"bg=gray\r\n" +
											"fg=red</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLInterface</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>200</x>\r\n" +
											"      <y>8</y>\r\n" +
											"      <w>64</w>\r\n" +
											"      <h>80</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>Interface\r\n" +
											"--\r\n" +
											"Operation1\r\n" +
											"Operation2</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLClass</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>16</x>\r\n" +
											"      <y>16</y>\r\n" +
											"      <w>80</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>SimpleClass</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Text</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>336</x>\r\n" +
											"      <y>168</y>\r\n" +
											"      <w>80</w>\r\n" +
											"      <h>56</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>This is a text element to place text anywhere.\r\n" +
											"style=wordwrap</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>96</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;&lt;-</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>112</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;&lt;.</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>128</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;-</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>144</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;..</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>152</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>40</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=-\r\n" +
											"m1=0..n\r\n" +
											"m2=0..1\r\n" +
											"teaches to &gt;</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;20.0;170.0;20.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>176</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>32</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;.&gt;\r\n" +
											"&lt;&lt;someStereotype&gt;&gt;</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;20.0;170.0;20.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>200</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>32</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;-\r\n" +
											"m1=0..n</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>224</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;&lt;&lt;-</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>240</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;&lt;&lt;&lt;-</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;170.0;10.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>192</x>\r\n" +
											"      <y>256</y>\r\n" +
											"      <w>152</w>\r\n" +
											"      <h>40</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=[Qualification]&lt;-\r\n" +
											"m2=1..5,6\r\n" +
											"</panel_attributes>\r\n" +
											"    <additional_attributes>40.0;20.0;170.0;20.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>88</x>\r\n" +
											"      <y>192</y>\r\n" +
											"      <w>88</w>\r\n" +
											"      <h>64</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;.\r\n" +
											"&lt;&lt;instanceOf&gt;&gt;</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;10.0;60.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>56</x>\r\n" +
											"      <y>328</y>\r\n" +
											"      <w>72</w>\r\n" +
											"      <h>64</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;.\r\n" +
											"&lt;&lt;include&gt;&gt;</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;10.0;60.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>88</x>\r\n" +
											"      <y>352</y>\r\n" +
											"      <w>64</w>\r\n" +
											"      <h>48</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=.&gt;\r\n" +
											"&lt;&lt;extends&gt;&gt;</panel_attributes>\r\n" +
											"    <additional_attributes>50.0;10.0;20.0;40.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>56</x>\r\n" +
											"      <y>400</y>\r\n" +
											"      <w>24</w>\r\n" +
											"      <h>64</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;&lt;.</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;10.0;10.0;60.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>88</x>\r\n" +
											"      <y>400</y>\r\n" +
											"      <w>72</w>\r\n" +
											"      <h>48</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes/>\r\n" +
											"    <additional_attributes>10.0;10.0;70.0;40.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>312</x>\r\n" +
											"      <y>32</y>\r\n" +
											"      <w>144</w>\r\n" +
											"      <h>48</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=&lt;&lt;-\r\n" +
											"a rose is a rose</panel_attributes>\r\n" +
											"    <additional_attributes>30.0;40.0;70.0;40.0;70.0;10.0;10.0;10.0;10.0;30.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"  <element>\r\n" +
											"    <id>Relation</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>8</x>\r\n" +
											"      <y>512</y>\r\n" +
											"      <w>336</w>\r\n" +
											"      <h>64</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>lt=-\r\n" +
											"r1=role A\\nrole B\r\n" +
											"m1=msg A\\nmsg B\r\n" +
											"r2=role C\\nrole D\r\n" +
											"m2=msg C\\nmsg D\r\n" +
											"m1pos=10,0\r\n" +
											"r2pos=-13,-4\r\n" +
											"multiple lines are possible\r\n" +
											"and label positions can\r\n" +
											"be customized</panel_attributes>\r\n" +
											"    <additional_attributes>10.0;40.0;400.0;40.0</additional_attributes>\r\n" +
											"  </element>\r\n" +
											"</diagram>\r\n" +
											"";
	private final String paletteContent2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" +
											"<diagram program=\"umlet\" version=\"14.2.0\">\r\n" +
											"  <zoom_level>8</zoom_level>\r\n" +
											"  <element>\r\n" +
											"    <id>UMLClass</id>\r\n" +
											"    <coordinates>\r\n" +
											"      <x>104</x>\r\n" +
											"      <y>16</y>\r\n" +
											"      <w>80</w>\r\n" +
											"      <h>24</h>\r\n" +
											"    </coordinates>\r\n" +
											"    <panel_attributes>/AbstractClass/\r\n" +
											"</panel_attributes>\r\n" +
											"    <additional_attributes/>\r\n" +
											"  </element>\r\n" +
											"</diagram>\r\n" +
											"";

	private final Diagram diagram = new SWTDiagramHandler();

	private final IElementFactory factory = new SWTElementFactory();

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		log.info("Call editor.init() " + uuid.toString());
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		diagramFile = getFile(input);
		DiagramIO.readFromString(paletteContent, diagram, factory);
		try { // use invokeAndWait to make sure the initialization is finished before SWT proceeds
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() { // initialize embedded panel here (and not in createPartControl) to avoid ugly scrollbars
					embeddedPanel = guiComponents.initEclipseGui();
				}
			});
		} catch (Exception e) {
			throw new PartInitException("Create DiagramHandler interrupted", e);
		}
	}

	private File getFile(IEditorInput input) throws PartInitException {
		if (input instanceof IFileEditorInput) { // Files opened from workspace
			return ((IFileEditorInput) input).getFile().getLocation().toFile();
		}
		else if (input instanceof org.eclipse.ui.ide.FileStoreEditorInput) { // Files from outside of the workspace (eg: edit current palette)
			return new File(((org.eclipse.ui.ide.FileStoreEditorInput) input).getURI());
		}
		else {
			throw new PartInitException("Editor input not supported.");
		}
	}

	@Override
	public boolean isDirty() {
		return handler.isChanged();
	}

	@Override
	public void createPartControl(Composite parent) {
		getGui().setCurrentEditor(Editor.this); // must be done before initialization of DiagramHandler (eg: to set propertypanel text)
		handler = new DiagramHandler(diagramFile);
		getGui().registerEditorForDiagramHandler(Editor.this, handler);
		getGui().setCurrentDiagramHandler(handler); // must be also set here because onFocus is not always called (eg: tab is opened during eclipse startup)
		getGui().open(handler);

		log.info("Call editor.createPartControl() " + uuid.toString());

		// create the three panels (to be decided if they should be moved to views...)
		// + sash (horizontal split)
		// +-- diagram pane (embed swing)
		// +-- sash (vertical split)
		// +----- palette pane : swt canvas
		// +----- properties, jface TextViewer
		mainForm = new SashForm(parent, SWT.HORIZONTAL);
		mainForm.setLayout(new FillLayout());
		mainComposite = new Composite(mainForm, SWT.EMBEDDED);
		mainFrame = SWT_AWT.new_Frame(mainComposite);
		sidebarForm = new SashForm(mainForm, SWT.VERTICAL);
		paletteCanvas = new Canvas(sidebarForm, SWT.NONE);
		propertiesTextViewer = new TextViewer(sidebarForm, SWT.NONE);
		propertiesTextViewer.setInput(new Document("TODO: properties"));
		swtOwnPropertyPane = new SWTOwnPropertyPane(propertiesTextViewer);
		propertiesTextViewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				swtOwnPropertyPane.updateGridElement();
			}
		});

		paletteCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				List<GridElement> gridElements = diagram.getGridElements();
				for (GridElement gridElement : gridElements) {
					com.baselet.element.interfaces.Component component = gridElement.getComponent();
					SWTComponent swtComp = (SWTComponent) component;
					swtComp.drawOn(e.gc, false, 1d);
				}
			}
		});

		// Bug 228221 - SWT no longer receives key events in KeyAdapter when using SWT_AWT.new_Frame AWT frame
		// Use a RootPaneContainer e.g. JApplet to embed the swing panel in the SWT part
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=228221
		// http://www.eclipse.org/articles/article.php?file=Article-Swing-SWT-Integration/index.html
		// The proposal to use JApplet instead of JPanel no longer works (eclipse photon java 8 and 11),
		// key events are only partially propagated to the underlying SWT event queue.
		JApplet applet = new JApplet();
		applet.setLayout(new BorderLayout());
		applet.setFocusCycleRoot(false);
		applet.add(embeddedPanel, BorderLayout.CENTER);
		mainFrame.add(applet);
		mainFrame.addWindowFocusListener(new UmletWindowFocusListener());

		// Leaving the swing context is not sufficient to return the processing of the key events to the
		// SWT event queue. Therefore install a WindowFocusListener, that will force the SWT shell to
		// receive the focus again.
		// This is a workaround. Even better would be to fix the bug in the event processing.
		mainFrame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				if (!mainComposite.isDisposed()) {
					mainComposite.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {

							boolean awtHasFocus = false;
							for (Window w : Window.getWindows()) {
								boolean isDiagramEditor = false;
								Component[] cs = w.getComponents();
								for (Component c : cs) {
									// Check if the window contains an applet. In this case we have
									// an diagram editor and activating the shell does not harm,
									// because the focus remains in the editor.
									if (c instanceof JApplet) {
										isDiagramEditor = true;
										break;
									}
								}

								// If another AWT window (dialog) is active, do not steal the focus.
								// The swing editor components are always visible, therefore we must
								// check visibility only for windows that contain no editor.
								if (w.isVisible() && !isDiagramEditor) {
									awtHasFocus = true;
									break;
								}
							}

							// force the focus to the SWT shell, but only if no
							// other swing compoment is active
							if (!awtHasFocus) {
								mainComposite.getShell().forceActive();
							}
						}
					});
				}
			}
		});

		// The event processing is platform depending. Therefore this listener may cause a duplicate
		// execution of actions on macos or windows.
		if ("gtk".equals(SWT.getPlatform())) {

			// If an AWT component has focus, KeyEvents are no longer passed to the
			// SWT event thread. As a workaround transform the AWT KeyEvent to an
			// SWT KeyEvent and pass it to the eclipse KeyBindingDispatcher. The
			// dispatcher will resolve a KeyStroke to an action and execute it.
			embeddedPanel.addKeyListener(new java.awt.event.KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {

					log.info("key typed (AWT component): " + e.toString());
					final Event swtEvent = convertKeyEvent(e);

					final KeyBindingDispatcher kbd = Workbench.getInstance().getContext().get(KeyBindingDispatcher.class);
					final List<KeyStroke> kss = KeyBindingDispatcher.generatePossibleKeyStrokes(swtEvent);

					if (kss.size() > 0) {
						StringBuilder msg = new StringBuilder("Found key binding: ");

						// For me that would be a reason to upgrade to jdk8 ;-)
						// msg.append(kss.stream().map((ks) -> ks.format()).collect(Collectors.joining(" | ")));
						for (int i = 0; i < kss.size(); i++) {
							if (i > 0) {
								msg.append(" | ");
							}
							msg.append(kss.get(i).toString());
						}
						log.info(msg.toString());

						// Actions that modify AWT components should run
						// in the AWT thread, that is the thread that called
						// this listener. But sometimes we end up with an
						// InvalidThreadAccess even if only AWT components are
						// effected.
						// Because AWT is much more forgiving, we use the SWT thread
						// for all actions including modifications of AWT components.
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								kbd.press(kss, swtEvent);
							}
						});
					}
				}
			});

			mainComposite.addKeyListener(new org.eclipse.swt.events.KeyAdapter() {
				@Override
				public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
					log.trace("key typed (SWT widget): " + e.toString());
				}
			});

			mainComposite.getShell().getDisplay().addFilter(SWT.KeyDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					log.trace("key down (SWT global): " + event.toString());
				}
			});
		}
	}

	private int convertAWTModifiers(int mods) {
		int modifiers = 0;
		if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) {
			modifiers |= SWT.SHIFT;
		}
		if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
			modifiers |= SWT.CTRL;
		}
		if ((mods & InputEvent.ALT_DOWN_MASK) != 0) {
			modifiers |= SWT.ALT;
		}
		return modifiers;
	}

	private org.eclipse.swt.widgets.Event convertKeyEvent(java.awt.event.KeyEvent e) {

		Event swtEvent = new Event();
		swtEvent.type = SWT.KeyDown;
		if (e.isControlDown()) {
			swtEvent.character = (char) ('a' - 1 + e.getKeyChar());
		}
		else {
			swtEvent.character = e.getKeyChar();
		}
		swtEvent.stateMask = convertAWTModifiers(e.getModifiersEx());
		swtEvent.keyCode = e.getKeyChar() | swtEvent.stateMask;
		swtEvent.widget = mainComposite;
		return swtEvent;
	}

	private EclipseGUI getGui() {
		return (EclipseGUI) CurrentGui.getInstance().getGui();
	}

	@Override
	public void setFocus() {
		log.info("Call editor.setFocus() " + uuid.toString());

		getGui().setCurrentEditor(this);
		getGui().setCurrentDiagramHandler(handler);
		if (handler != null) {
			handler.getDrawPanel().getSelector().updateSelectorInformation();
		}

		// TODO: need to update the palette content? (one instance per editor?)
		// Display.getDefault().syncExec(new Runnable() {
		// @Override
		// public void run() {
		// /**
		// * usually the palettes get lost after switching the editor (for unknown reasons but perhaps because the Main class is build for exactly one palette (like in standalone umlet) but here every tab has its own palette)
		// * Therefore recreate them and also reselect the current palette and repaint every element with scrollbars (otherwise they have a visual error)
		// */
		// if (guiComponents.getPalettePanel().getComponentCount() == 0) {
		// for (PaletteHandler palette : Main.getInstance().getPalettes().values()) {
		// guiComponents.getPalettePanel().add(palette.getDrawPanel().getScrollPane(), palette.getName());
		// palette.getDrawPanel().getScrollPane().invalidate();
		// }
		// }
		// showPalette(getSelectedPaletteName());
		// getGui().setValueOfZoomDisplay(handler.getGridSize());
		// guiComponents.getPropertyTextPane().invalidate();
		// }
		// });
	}

	public DrawPanel getDiagram() {
		if (handler == null) {
			return null;
		}
		return handler.getDrawPanel();
	}

	@Override
	public void dispose() {
		super.dispose();
		log.info("Call editor.dispose( )" + uuid.toString());
		// AB: The eclipse plugin might hang sometimes if this section is not placed into an event queue, since swing or swt is not thread safe!
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// TODO@fab MailPanel support
				// if (guiComponents.getMailPanel().isVisible()) {
				// guiComponents.getMailPanel().closePanel();
				// }
				getGui().editorRemoved(Editor.this);
			}
		});
	}

	public void setCursor(Cursor cursor) {
		embeddedPanel.setCursor(cursor);
	}

	public OwnSyntaxPane getPropertyPane() {
		return swtOwnPropertyPane;
		// return guiComponents.getPropertyTextPane();
	}

	public JTextComponent getCustomPane() {
		return guiComponents.getCustomPanel().getTextPane();
	}

	public void requestFocus() {
		embeddedPanel.requestFocus();
	}

	public Frame getMainFrame() {
		return mainFrame;
	}

	public void dirtyChanged() {
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
	}

	public void diagramNameChanged() {
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		});
	}

	public CustomElementHandler getCustomElementHandler() {
		return guiComponents.getCustomHandler();
	}

	public void setMailPanelEnabled(boolean enable) {
		// TODO:fab mail support?
		// guiComponents.setMailPanelEnabled(enable);
	}

	public boolean isMailPanelVisible() {
		// TODO:fab mail support?
		return false;
		// return guiComponents.getMailPanel().isVisible();
	}

	public String getSelectedPaletteName() {
		// TODO@fab combo for palette list missing
		return "";
		// return guiComponents.getPaletteList().getSelectedItem().toString();
	}

	public int getMainSplitLocation() {
		// TODO:fab widget is disposed here
		return 0;
		// return mainForm.getWeights()[0];
		// return guiComponents.getMainSplit().getDividerLocation();
	}

	public int getRightSplitLocation() {
		// TODO:fab widget is disposed here
		return 0;
		// return sidebarForm.getWeights()[0];
		// return guiComponents.getRightSplit().getDividerLocation();
	}

	public int getMailSplitLocation() {
		// TODO:fab mail support?
		return 0;
		// return guiComponents.getMailSplit().getDividerLocation();
	}

	public void showPalette(final String paletteName) {
		guiComponents.setPaletteActive(paletteName);
	}

	public void setCustomPanelEnabled(boolean enable) {
		guiComponents.setCustomPanelEnabled(enable);
		setDrawPanelEnabled(!enable);
	}

	private void setDrawPanelEnabled(boolean enable) {
		handler.getDrawPanel().getScrollPane().setEnabled(enable);
	}

	public void focusPropertyPane() {
		guiComponents.getPropertyTextPane().getTextComponent().requestFocus();
	}

	public void open(final DiagramHandler handler) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				guiComponents.setContent(handler.getDrawPanel().getScrollPane());
			}
		});
	}

}
