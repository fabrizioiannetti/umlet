package com.baselet.plugin.swt;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import com.baselet.control.constants.Constants;
import com.baselet.control.enums.Program;
import com.baselet.control.util.Path;

public class SWTMain {
	private static SWTMain swtMain = new SWTMain();
	private TreeMap<String, SWTPaletteHandler> palettes;

	public static SWTMain getInstance() {
		return swtMain;
	}

	public TreeMap<String, SWTPaletteHandler> getPalettes() {
		if (palettes == null) {
			palettes = new TreeMap<String, SWTPaletteHandler>(Constants.DEFAULT_FIRST_COMPARATOR);
			// scan palettes
			List<File> palettes = scanForPalettes();
			for (File palette : palettes) {
				this.palettes.put(getFilenameWithoutExtension(palette), new SWTPaletteHandler(palette));
			}
		}
		return palettes;
	}

	private List<File> scanForPalettes() {
		// scan palettes directory...
		List<File> palettes = new ArrayList<File>();
		File palettesDir = new File(Path.homeProgram(), "palettes");
		Collections.addAll(palettes, palettesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("." + Program.getInstance().getExtension());
			}
		}));
		return palettes;
	}

	private String getFilenameWithoutExtension(File file) {
		return file.getName().substring(0, file.getName().indexOf("."));
	}
}
