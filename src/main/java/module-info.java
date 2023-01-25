module org.baxter.disco.ocr {
    requires com.pi4j;
    requires com.pi4j.plugin.raspberrypi;
    requires com.pi4j.plugin.pigpio;
    requires com.pi4j.library.pigpio;
    requires javafx.fxml;
    requires org.apache.poi.poi;
    requires org.apache.commons.configuration2;
    requires org.apache.xmlbeans;
    requires org.bytedeco.tesseract;
    requires org.bytedeco.opencv;
    requires org.bytedeco.javacpp;
    requires javafx.graphics;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.ooxml.schemas;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;

    requires org.bytedeco.javacv.platform;
    requires java.desktop;

    uses com.pi4j.extension.Extension;
    uses com.pi4j.provider.Provider;

    // allow access to classes in the following namespaces for Pi4J annotation processing
    opens org.baxter.disco.ocr to com.pi4j;

    //exports org.baxter.disco.ocr;
}
