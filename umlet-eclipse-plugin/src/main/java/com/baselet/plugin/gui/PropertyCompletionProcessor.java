package com.baselet.plugin.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;

import com.baselet.control.enums.ElementId;
import com.baselet.element.interfaces.GridElement;
import com.baselet.gui.AutocompletionText;

final class PropertyCompletionProcessor extends TemplateCompletionProcessor {
	private final String DEFAULT_CONTEXT_TYPE = "defaultContextType";
	private String contextTypeId = DEFAULT_CONTEXT_TYPE;
	private final Map<ElementId, List<Template>> templatesForElement = new HashMap<ElementId, List<Template>>();

	public PropertyCompletionProcessor() {
	}

	public void forElement(GridElement element) {
		if (element != null) {
			if (!templatesForElement.containsKey(element.getId())) {
				templatesForElement.put(element.getId(), getTemplatesForElement(element));
			}
			contextTypeId = element.getId().name();
		}
		else {
			contextTypeId = DEFAULT_CONTEXT_TYPE;
		}
	}

	private List<Template> getTemplatesForElement(GridElement e) {
		List<Template> templates = new ArrayList<Template>();
		if (e != null) {
			for (AutocompletionText autocompletionText : e.getAutocompletionList()) {
				String name = autocompletionText.getText();
				String description = autocompletionText.getInfo();
				String pattern = name;
				templates.add(new Template(name, description, e.getId().name(), pattern, true));
			}
		}
		return templates;
	}

	@Override
	protected Template[] getTemplates(String contextTypeId) {
		if (!DEFAULT_CONTEXT_TYPE.equals(contextTypeId)) {
			try {
				final ElementId id = ElementId.valueOf(contextTypeId);
				return templatesForElement.get(id).toArray(new Template[0]);
			} catch (IllegalArgumentException e) {
				// should never get a type id that is not in the enum
				e.printStackTrace();
			}
		}
		return new Template[0];
	}

	@Override
	protected Image getImage(Template template) {
		return null;
	}

	@Override
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return new TemplateContextType(contextTypeId);
	}
}