package com.baselet.plugin.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.element.Selector;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.interfaces.HasGridElements;
import com.baselet.element.old.custom.CustomElementHandler;
import com.baselet.plugin.gui.EclipseGUI.Pane;
import com.baselet.plugin.swt.DiagramIO;
import com.baselet.plugin.swt.IElementFactory;
import com.baselet.plugin.swt.SWTDiagramHandler;
import com.baselet.plugin.swt.SWTElementFactory;

public class Editor extends EditorPart {

	private static final Logger log = LoggerFactory.getLogger(Editor.class);

	private final UUID uuid = UUID.randomUUID();

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO@fab
		monitor.done();
	}

	@Override
	public void doSaveAs() {
		// TODO@fab
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	private SashForm mainForm;

	private SashForm sidebarForm;

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
	private final String paletteContent3 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" +
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
	private final Diagram paletteDiagram = new SWTDiagramHandler();

	private final IElementFactory factory = new SWTElementFactory();

	private DiagramViewer paletteViewer;

	private DiagramViewer diagramViewer;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		log.info("Call editor.init() " + uuid.toString());
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		DiagramIO.readFromFile(getFile(input), diagram, factory);
		DiagramIO.readFromString(paletteContent, paletteDiagram, factory);
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
		// TODO@fab
		return false;
	}

	public class EclipseEditorSelector extends Selector {

		private final HasGridElements gridElementProvider;

		public EclipseEditorSelector(HasGridElements gridElementProvider) {
			this.gridElementProvider = gridElementProvider;
		}

		private final List<GridElement> selectedElements = new ArrayList<GridElement>();

		@Override
		public List<GridElement> getSelectedElements() {
			return selectedElements;
		}

		@Override
		public List<GridElement> getAllElements() {
			return gridElementProvider.getGridElements();
		}

		@Override
		public void doAfterSelect(GridElement e) {
			super.doAfterSelect(e);
			swtOwnPropertyPane.switchToElement(e);
		}
	}

	private class PaneTypeOnFocus extends FocusAdapter {
		private final Pane pane;

		public PaneTypeOnFocus(Pane pane) {
			super();
			this.pane = pane;
		}

		@Override
		public void focusGained(FocusEvent e) {
			getGui().setPaneFocused(pane);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		getGui().setCurrentEditor(Editor.this); // must be done before initialization of DiagramHandler (eg: to set propertypanel text)

		log.info("Call editor.createPartControl() " + uuid.toString());

		// create the three panels (to be decided if they should be moved to views...)
		// + sash (horizontal split)
		// +-- diagram pane (embed swing)
		// +-- sash (vertical split)
		// +----- palette pane : swt canvas
		// +----- properties, jface TextViewer
		mainForm = new SashForm(parent, SWT.HORIZONTAL);
		mainForm.setLayout(new FillLayout());
		diagramViewer = new DiagramViewer(mainForm);
		sidebarForm = new SashForm(mainForm, SWT.VERTICAL);
		paletteViewer = new DiagramViewer(sidebarForm);
		propertiesTextViewer = new TextViewer(sidebarForm, SWT.V_SCROLL | SWT.H_SCROLL);
		propertiesTextViewer.setInput(new Document("TODO: properties"));
		swtOwnPropertyPane = new SWTOwnPropertyPane(propertiesTextViewer);
		propertiesTextViewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				swtOwnPropertyPane.updateGridElement();
			}
		});
		propertiesTextViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.PROPERTY));
		paletteViewer.setInput(paletteDiagram);
		paletteViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.DIAGRAM));

		MenuManager menuManager = new MenuManager();
		menuManager.add(ActionFactory.COPY.create(getSite().getWorkbenchWindow()));
		menuManager.add(ActionFactory.CUT.create(getSite().getWorkbenchWindow()));
		menuManager.add(ActionFactory.PASTE.create(getSite().getWorkbenchWindow()));
		menuManager.add(ActionFactory.SELECT_ALL.create(getSite().getWorkbenchWindow()));
		Menu menu = menuManager.createContextMenu(propertiesTextViewer.getControl());
		propertiesTextViewer.getControl().setMenu(menu);

		diagramViewer.setInput(diagram);
		diagramViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.DIAGRAM));
	}

	private EclipseGUI getGui() {
		return EclipseGUI.getCurrent();
	}

	@Override
	public void setFocus() {
		log.info("Call editor.setFocus() " + uuid.toString());
		getGui().setCurrentEditor(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		log.info("Call editor.dispose( )" + uuid.toString());
	}

	public SWTOwnPropertyPane getPropertyPane() {
		return swtOwnPropertyPane;
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
		return null;
	}

	public void focusPropertyPane() {
		propertiesTextViewer.getControl().setFocus();
	}
}
