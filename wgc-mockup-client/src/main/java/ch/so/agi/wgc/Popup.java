package ch.so.agi.wgc;

import static elemental2.dom.DomGlobal.console;

import static org.jboss.elemento.Elements.a;
import static org.jboss.elemento.Elements.body;
import static org.jboss.elemento.Elements.div;
import static org.jboss.elemento.Elements.table;
import static org.jboss.elemento.Elements.tbody;
import static org.jboss.elemento.Elements.tr;
import static org.jboss.elemento.Elements.td;
import static org.jboss.elemento.Elements.img;
import static org.jboss.elemento.Elements.span;
import static org.jboss.elemento.EventType.*;
import static org.dominokit.domino.ui.style.Unit.px;

import java.util.ArrayList;
import java.util.List;

import org.dominokit.domino.ui.button.Button;
import org.dominokit.domino.ui.button.ButtonSize;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.style.Color;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;
import org.jboss.elemento.Attachable;
import org.jboss.elemento.HtmlContentBuilder;
import org.jboss.elemento.IsElement;

import com.google.gwt.user.client.Window;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.Text;
import com.google.gwt.xml.client.XMLParser;

import elemental2.dom.DomGlobal;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLTableElement;
import elemental2.dom.Headers;
import elemental2.dom.Location;
import elemental2.dom.MutationRecord;
import elemental2.dom.RequestInit;

import ol.Coordinate;
import ol.Extent;
import ol.Feature;
import ol.FeatureOptions;
import ol.Map;
import ol.MapBrowserEvent;
import ol.OLFactory;
import ol.Overlay;
import ol.OverlayOptions;
import ol.View;
import ol.format.GeoJson;
import ol.format.Wkt;
import ol.geom.Geometry;
import ol.layer.Base;
import ol.layer.Image;
import ol.layer.LayerOptions;
import ol.layer.VectorLayerOptions;
import ol.source.ImageWms;
import ol.source.ImageWmsOptions;
import ol.source.ImageWmsParams;
import ol.source.Vector;
import ol.source.VectorOptions;
import ol.style.Fill;
import ol.style.Stroke;
import ol.style.Style;

public class Popup implements IsElement<HTMLElement>, Attachable {
    private final HTMLElement root;
    
    private String ID_ATTR_NAME = "id";
    private String HIGHLIGHT_VECTOR_LAYER_ID = "highlight_vector_layer";
    private String HIGHLIGHT_VECTOR_FEATURE_ID = "highlight_fid";

    private final String LAYERS = "ch.so.agi.av.grundstuecke.rechtskraeftig,ch.so.agi.av.grundstuecke.projektierte";
    private final String BASE_URL_FEATUREINFO = "https://geo.so.ch/api/v1/featureinfo/somap?service=WMS&version=1.3.0&request=GetFeatureInfo&x=51&y=51&i=51&j=51&height=101&width=101&srs=EPSG:2056&crs=EPSG:2056&info_format=text%2Fxml&with_geometry=true&with_maptip=false&feature_count=100&FI_POINT_TOLERANCE=8&FI_LINE_TOLERANCE=8&FI_POLYGON_TOLERANCE=4";
    private final String BASE_URL_REPORT = "https://geo.so.ch/api/v1/document/";  
    //baseUrlBigMap: https://geo.so.ch/map/
    
    private Map map;

    public Popup(Map map, MapBrowserEvent event) {
        this.map = map;
        removeHighlightVectorLayer();
        
        HTMLElement icon = Icons.ALL.close().setId("popupCloseIcon").element();
        icon.style.verticalAlign = "middle";
        icon.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(elemental2.dom.Event evt) {
                removeHighlightVectorLayer(); 
                root.remove();
            }
        });

        HTMLElement closeButton = span().id("popupHeaderButtonSpan").add(icon).element();                    
        HtmlContentBuilder<HTMLDivElement> popupBuilder = div().id("popup");
        popupBuilder.add(
                div().id("popupHeader")
                .add(span().id("popupHeaderTextSpan").textContent("Objektinformation"))
                .add(closeButton)
                ); 
        
        root = (HTMLDivElement) popupBuilder.element();
        root.hidden = true;
//        root.style.position = "absolute";
//        root.style.top = "5px";
//        root.style.left = "5px";

        double resolution = map.getView().getResolution();

        // 50/51/101-Ansatz ist anscheinend bei OpenLayers normal.
        // -> siehe baseUrlFeatureInfo resp. ein Original-Request
        // im Web GIS Client.
        double minX = event.getCoordinate().getX() - 50 * resolution;
        double maxX = event.getCoordinate().getX() + 51 * resolution;
        double minY = event.getCoordinate().getY() - 50 * resolution;
        double maxY = event.getCoordinate().getY() + 51 * resolution;

        String urlFeatureInfo = BASE_URL_FEATUREINFO + "&layers=" + LAYERS;
        urlFeatureInfo += "&query_layers=" + LAYERS;
        urlFeatureInfo += "&bbox=" + minX + "," + minY + "," + maxX + "," + maxY;

        RequestInit requestInit = RequestInit.create();
        Headers headers = new Headers();
        headers.append("Content-Type", "application/x-www-form-urlencoded"); 
        requestInit.setHeaders(headers);
        
        DomGlobal.fetch(urlFeatureInfo)
        .then(response -> {
            if (!response.ok) {
                return null;
            }
            return response.text();
        })
        .then(xml -> {
            Document messageDom = XMLParser.parse(xml);
            
            if (messageDom.getElementsByTagName("Feature").getLength() == 0) {
                root.appendChild(div().css("popupNoContent").textContent("Keine weiteren Informationen").element());
            }

            List<Feature> featureList = new ArrayList<Feature>();
            for (int i=0; i<messageDom.getElementsByTagName("Layer").getLength(); i++) {
                Node layerNode = messageDom.getElementsByTagName("Layer").item(i);
                String layerName = ((com.google.gwt.xml.client.Element) layerNode).getAttribute("layername"); 
                String layerTitle = ((com.google.gwt.xml.client.Element) layerNode).getAttribute("name"); 
                                
                if (layerNode.getChildNodes().getLength() == 0) {
                    continue;
                };
                
                NodeList featureNodes = ((com.google.gwt.xml.client.Element) layerNode).getElementsByTagName("Feature");
                for (int m=0; m<featureNodes.getLength(); m++) {
                    com.google.gwt.xml.client.Element featureElement = ((com.google.gwt.xml.client.Element) featureNodes.item(m));
                    String featureId = featureElement.getAttribute("id");
                    
                    // TODO: Hardcodiert, weil falsch konfiguriert bei uns.
                    if (layerName.equalsIgnoreCase("ch.so.agi.av.grundstuecke.projektierte")) {
                        layerTitle = "projektierte Grundstücke";
                    }
                    
                    HtmlContentBuilder<HTMLDivElement> featureContentBuilder = div();
                    featureContentBuilder.add(div().css("popupLayerHeader").textContent(layerTitle).element());
                    
                    HtmlContentBuilder<HTMLDivElement> popupContentBuilder = div().id(featureId).css("popupContent");
                    
                    HtmlContentBuilder<HTMLTableElement> tableBuilder = table().css("attribute-list");
                    NodeList attributeNodes = featureElement.getElementsByTagName("Attribute");
                    for (int n=0; n<attributeNodes.getLength(); n++) {
                            com.google.gwt.xml.client.Element attrElement = ((com.google.gwt.xml.client.Element) attributeNodes.item(n));
                            String attrName = attrElement.getAttribute("name");
                            String attrValue = attrElement.getAttribute("value");
                            
                            if (attrName.equalsIgnoreCase("geometry")) {
                                FeatureOptions featureOptions = OLFactory.createOptions();
                                featureOptions.setGeometry(new Wkt().readGeometry(attrValue));
                                Feature feature = new Feature(featureOptions);
                                feature.setId(featureId);
                                featureList.add(feature);

                                bind(featureContentBuilder.element(), mouseenter, popupEvent -> {
                                    popupContentBuilder.element().style.backgroundColor = "rgba(249,128,0,0.3)";
                                    toggleFeatureFill(featureId);
                                });
                                
                                bind(featureContentBuilder.element(), mouseleave, popupEvent -> {
                                    popupContentBuilder.element().style.backgroundColor = "white";
                                    toggleFeatureFill(featureId);
                                });

                                continue;
                            }
                            // TODO: Warum taucht das im GetFeatureInfo auf?
                            if (attrName.equalsIgnoreCase("bfs_nr")) {
                                continue;
                            }
                            
                            tableBuilder
                            .add(
                                    tr()
                                    .add(
                                            td().css("identify-attr-title wrap").add(attrName + ":"))
                                    .add(
                                            td().css("identify-attr-value wrap").add(attrValue))
                                );
                    }
                    popupContentBuilder.add(tableBuilder.element());
                    featureContentBuilder.add(popupContentBuilder.element());
                    
                    root.appendChild(featureContentBuilder.element());
                    root.hidden = false;   
                }
            }            
            createHighlightVectorLayer(featureList);
            return null;
        })
        .catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error);
            return null;
        });
    }
    
    @Override
    public void attach(MutationRecord mutationRecord) {}
    
    @Override
    public HTMLElement element() {
        return root;
    }
    
    private void toggleFeatureFill(String id) {
        ol.layer.Vector vectorLayer = (ol.layer.Vector) getMapLayerById(HIGHLIGHT_VECTOR_LAYER_ID);
        Vector vectorSource = vectorLayer.getSource();
        Feature feature = vectorSource.getFeatureById(id);
        
        Style style = new Style();
        Stroke stroke = new Stroke();
        stroke.setWidth(4);
        stroke.setColor(new ol.color.Color(249, 128, 0, 1.0));
        style.setStroke(stroke);
        Fill fill = new Fill();
        if (feature.get("highlighted") != null && (boolean) feature.get("highlighted")) {
            fill.setColor(new ol.color.Color(255, 255, 80, 0.6));
            feature.set("highlighted", false);

        } else {
            fill.setColor(new ol.color.Color(249, 128, 0, 1.0));
            feature.set("highlighted", true);
        }
        style.setFill(fill);
        feature.setStyle(style);        
    }
        
    private void createHighlightVectorLayer(List<Feature> features) {
        Style style = new Style();
        Stroke stroke = new Stroke();
        stroke.setWidth(4);
        stroke.setColor(new ol.color.Color(249, 128, 0, 1.0));
        //stroke.setColor(new ol.color.Color(230, 0, 0, 0.6));
        style.setStroke(stroke);
        Fill fill = new Fill();
        fill.setColor(new ol.color.Color(255, 255, 80, 0.6));
        style.setFill(fill);

        ol.Collection<Feature> featureCollection = new ol.Collection<Feature>();
        for (Feature feature : features) {
            feature.setStyle(style);
            featureCollection.push(feature);
        }

        VectorOptions vectorSourceOptions = OLFactory.createOptions();
        vectorSourceOptions.setFeatures(featureCollection);
        Vector vectorSource = new Vector(vectorSourceOptions);
        
        VectorLayerOptions vectorLayerOptions = OLFactory.createOptions();
        vectorLayerOptions.setSource(vectorSource);
        ol.layer.Vector vectorLayer = new ol.layer.Vector(vectorLayerOptions);
        vectorLayer.set(ID_ATTR_NAME, HIGHLIGHT_VECTOR_LAYER_ID);
        map.addLayer(vectorLayer);
    }
    
//    private ol.layer.Vector createHighlightVectorLayer(String geometry) {
//        Geometry highlightGeometry = new Wkt().readGeometry(geometry);
//        return createHighlightVectorLayer(highlightGeometry);
//    }
//
//    private ol.layer.Vector createHighlightVectorLayer(Geometry geometry) {
//        FeatureOptions featureOptions = OLFactory.createOptions();
//        featureOptions.setGeometry(geometry);
//
//        Feature feature = new Feature(featureOptions);
//        //feature.setId(REAL_ESTATE_VECTOR_FEATURE_ID);
//
//        Style style = new Style();
//        Stroke stroke = new Stroke();
//        stroke.setWidth(5);
//        stroke.setColor(new ol.color.Color(249, 128, 0, 1.0));
//        //stroke.setColor(new ol.color.Color(230, 0, 0, 0.6));
//        style.setStroke(stroke);
//        Fill fill = new Fill();
//        fill.setColor(new ol.color.Color(255, 255, 80, 0.6));
//        style.setFill(fill);
//        feature.setStyle(style);
//
//        ol.Collection<Feature> lstFeatures = new ol.Collection<Feature>();
//        lstFeatures.push(feature);
//
//        VectorOptions vectorSourceOptions = OLFactory.createOptions();
//        vectorSourceOptions.setFeatures(lstFeatures);
//        Vector vectorSource = new Vector(vectorSourceOptions);
//        
//        VectorLayerOptions vectorLayerOptions = OLFactory.createOptions();
//        vectorLayerOptions.setSource(vectorSource);
//        ol.layer.Vector vectorLayer = new ol.layer.Vector(vectorLayerOptions);
//        vectorLayer.set(ID_ATTR_NAME, HIGHLIGHT_VECTOR_LAYER_ID);
//
//        return vectorLayer;
//    }
    
    private void removeHighlightVectorLayer() {
        Base vlayer = getMapLayerById(HIGHLIGHT_VECTOR_LAYER_ID);
        map.removeLayer(vlayer);
    }

    private Base getMapLayerById(String id) {
        ol.Collection<Base> layers = map.getLayers();
        for (int i = 0; i < layers.getLength(); i++) {
            Base item = layers.item(i);
            try {
                String layerId = item.get(ID_ATTR_NAME);
                if (layerId == null) {
                    continue;
                }
                if (layerId.equalsIgnoreCase(id)) {
                    return item;
                }
            } catch (Exception e) {
                console.log(e.getMessage());
                console.log("should not reach here");
            }
        }
        return null;
    }

}
