package org.example.customcleanner.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FileItem {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty path = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();

    public FileItem(String name, String path, String size, String type, boolean selected) {
        this.name.set(name);
        this.path.set(path);
        this.size.set(size);
        this.type.set(type);
        this.selected.set(selected);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getPath() { return path.get(); }
    public StringProperty pathProperty() { return path; }

    public String getSize() { return size.get(); }
    public StringProperty sizeProperty() { return size; }

    public String getType() { return type.get(); }
    public StringProperty typeProperty() { return type; }

    public boolean isSelected() { return selected.get(); }
    public BooleanProperty selectedProperty() { return selected; }
}