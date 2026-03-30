module net.talaatharb.gsplat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.google.gson;
    requires jdk.httpserver;

    opens net.talaatharb.gsplat.ui to javafx.fxml;
    opens net.talaatharb.gsplat.model to com.google.gson;

    exports net.talaatharb.gsplat;
    exports net.talaatharb.gsplat.ui;
    exports net.talaatharb.gsplat.model;
    exports net.talaatharb.gsplat.service;
    exports net.talaatharb.gsplat.util;
}
