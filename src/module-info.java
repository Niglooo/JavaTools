open module nigloo.tools {
	requires java.logging;
	
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
	
	requires transitive com.google.gson;

	exports nigloo.tool;
	exports nigloo.tool.collection;
	exports nigloo.tool.gson;
	exports nigloo.tool.injection;
	exports nigloo.tool.injection.annotation;
	exports nigloo.tool.injection.impl;
	exports nigloo.tool.javafx.component;
	exports nigloo.tool.thread;
}