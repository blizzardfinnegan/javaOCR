module org.baxter.disco.ocr {
    requires org.slf4j;             //slf4j-api-2.0.0-alpha1.jar
    requires org.slf4j.simple;      //slf4j-simple-2.0.0-alpha1.jar & simplelogger.properties
    requires com.pi4j;
    requires com.pi4j.plugin.raspberrypi;
    requires com.pi4j.plugin.pigpio;
    requires com.pi4j.library.pigpio;
    requires javafx.fxml;
    requires org.apache.poi.poi;
    requires org.apache.commons.configuration2;
    requires org.bytedeco.tesseract;
    requires org.bytedeco.opencv;
    requires org.bytedeco.javacpp;
    requires javafx.graphics;

    requires org.bytedeco.javacv.platform;
    requires java.desktop;

    uses com.pi4j.extension.Extension;
    uses com.pi4j.provider.Provider;

    // allow access to classes in the following namespaces for Pi4J annotation processing
    opens org.baxter.disco.ocr to com.pi4j;

    //exports org.baxter.disco.ocr;
}
