package abi21_0_0.host.exp.exponent.modules.api.components.maps;

import android.app.Activity;

import abi21_0_0.com.facebook.react.ReactPackage;
import abi21_0_0.com.facebook.react.bridge.JavaScriptModule;
import abi21_0_0.com.facebook.react.bridge.NativeModule;
import abi21_0_0.com.facebook.react.bridge.ReactApplicationContext;
import abi21_0_0.com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapsPackage implements ReactPackage {
    public MapsPackage(Activity activity) {
    } // backwards compatibility

    public MapsPackage() {
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        return Arrays.<NativeModule>asList(new AirMapModule(reactContext));
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        AirMapCalloutManager calloutManager = new AirMapCalloutManager();
        AirMapMarkerManager annotationManager = new AirMapMarkerManager();
        AirMapPolylineManager polylineManager = new AirMapPolylineManager(reactContext);
        AirMapPolygonManager polygonManager = new AirMapPolygonManager(reactContext);
        AirMapCircleManager circleManager = new AirMapCircleManager(reactContext);
        AirMapManager mapManager = new AirMapManager(reactContext);
        AirMapLiteManager mapLiteManager = new AirMapLiteManager(reactContext);
        AirMapUrlTileManager tileManager = new AirMapUrlTileManager(reactContext);

        return Arrays.<ViewManager>asList(
                calloutManager,
                annotationManager,
                polylineManager,
                polygonManager,
                circleManager,
                mapManager,
                mapLiteManager,
                tileManager);
    }
}
