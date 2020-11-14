[![Build Status](https://api.travis-ci.org/umlet/umlet.svg?branch=master)](https://travis-ci.org/umlet/umlet)
# UMLet
UMLet is an open-source UML tool with a simple user interface: draw UML diagrams fast, export diagrams to eps, pdf, jpg, svg, and clipboard, share diagrams using Eclipse, and create new, custom UML elements. 

* Please check out the [Wiki](https://github.com/umlet/umlet/wiki) for frequently asked questions

* Go to http://www.umlet.com to get the latest compiled versions or to http://www.umletino.com to use UMLet in your web browser

## About this fork (SWT Fork)

This is a fork to experiment porting the GUI to SWT instead of embedding swing components
to achieve a better eclipse integration.

Currently supported (bar any bug):

* Painting main diagram and palette
* Attributes panel
* palette selection via popup menu
* selection: single, multiple, all, lasso
* copy/cut/paste, duplicate (also from palette) by double click
* move/resize by mouse and arrow keys
* undo/redo
* set fg/bg color via context menu
* editor reports dirty state
* save
* content assist in properties pane
* png, bmp, jpeg export (no DPI metadata support)
* render bold/italic text

To do:

* save as
* export diagram (svg, pdf, eps, gif)
* grouping
* zoom
* custom elements
* any subtle change...

known bugs:

* lw > 1 not rendered correctly

Not planned:

* email panel
* scrollbars in diagrams

limitations:

* only tested on linux (GTK)
