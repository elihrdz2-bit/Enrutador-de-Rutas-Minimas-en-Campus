import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.util.*;

public class RutaMinimaCampus3D extends Application {

    enum TipoLugar { EDIFICIO, CANCHA, PARQUEO }

    static class Edificio {
        String nombre;
        double x, z, alto;
        TipoLugar tipo;
        Edificio(String nombre, double x, double z, double alto, TipoLugar tipo) {
            this.nombre = nombre; this.x = x; this.z = z; this.alto = alto; this.tipo = tipo;
        }
    }

    static class Conexion {
        String origen, destino;
        int distancia;
        Conexion(String origen, String destino, int distancia) {
            this.origen = origen; this.destino = destino; this.distancia = distancia;
        }
    }

    static class Posicion2D {
        double x, y;
        Posicion2D(double x, double y) { this.x = x; this.y = y; }
    }

    private static final double PISO_Y = -5;

    private static final String COLOR_FONDO = "#121212";
    private static final String COLOR_PANEL = "#1c1c1f";
    private static final String COLOR_ACENTO = "#1DB975";
    private static final String COLOR_TEXTO = "#e8e8e8";

    private static final Color COLOR_EDIFICIO = Color.web("#585b63");
    private static final Color COLOR_CANCHA = Color.web("#2f6b3c");
    private static final Color COLOR_PARQUEO = Color.web("#3a3a3a");
    private static final Color COLOR_EDIFICIO_RUTA = Color.web(COLOR_ACENTO);
    private static final Color COLOR_CAMINO = Color.web("#787c86");
    private static final Color COLOR_CAMINO_RUTA = Color.web(COLOR_ACENTO);

    private static final double CAJA_2D_ANCHO = 90;
    private static final double CAJA_2D_ALTO = 34;

    private final Map<String, Edificio> edificios = new LinkedHashMap<>();
    private final List<Conexion> conexiones = new ArrayList<>();
    private final Map<String, List<String[]>> adyacencia = new HashMap<>();
    private final Map<String, Posicion2D> posiciones2D = new LinkedHashMap<>();

    private final Map<String, Box> cajasPorEdificio = new HashMap<>();
    private final Map<String, Color> colorNormalPorEdificio = new HashMap<>();
    private final Map<Conexion, Cylinder> cilindrosPorConexion = new LinkedHashMap<>();

    private List<String> rutaResaltada = new ArrayList<>();

    private final Rotate rotateX = new Rotate(-25, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-35, Rotate.Y_AXIS);
    private double anclaX, anclaY, anguloAnclaX, anguloAnclaY;
    private PerspectiveCamera camera;

    private Label labelResultado;
    private Canvas canvas2D;
    private TextArea areaCalculo;

    @Override
    public void start(Stage stage) {
        inicializarDatos();
        inicializarPosiciones2D();

        Group mundo = new Group();
        mundo.getTransforms().addAll(rotateY, rotateX);
        mundo.getChildren().add(crearPiso());

        for (Conexion c : conexiones) {
            Cylinder camino = crearCamino(edificios.get(c.origen), edificios.get(c.destino));
            cilindrosPorConexion.put(c, camino);
            mundo.getChildren().add(camino);
        }
        for (Edificio e : edificios.values()) {
            mundo.getChildren().add(crearEdificio(e));
        }

        Group root3D = new Group(mundo);
        root3D.getChildren().add(new AmbientLight(Color.color(0.95, 0.95, 0.95)));

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(6000);
        camera.setTranslateZ(-1500);
        camera.setTranslateY(-350);
        camera.setFieldOfView(35);
        root3D.getChildren().add(camera);

        SubScene subScene = new SubScene(root3D, 780, 560, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web(COLOR_FONDO));
        subScene.setCamera(camera);
        habilitarRotacionConMouse(subScene);
        habilitarZoomConRueda(subScene);

        VBox panel3D = new VBox(subScene);
        panel3D.setStyle(
            "-fx-background-color: " + COLOR_PANEL + ";" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);"
        );
        panel3D.setPadding(new javafx.geometry.Insets(6));

        canvas2D = new Canvas(540, 335);
        Label tituloPanel2D = new Label("Mapa 2D");
        tituloPanel2D.setFont(Font.font("System", FontWeight.BOLD, 15));
        tituloPanel2D.setStyle("-fx-text-fill: " + COLOR_TEXTO + ";");

        VBox panel2D = new VBox(10, tituloPanel2D, canvas2D);
        panel2D.setPadding(new javafx.geometry.Insets(16));
        panel2D.setStyle(
            "-fx-background-color: " + COLOR_PANEL + ";" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);"
        );

        HBox centro = new HBox(16, panel2D, panel3D);
        centro.setPadding(new javafx.geometry.Insets(16));
        centro.setStyle("-fx-background-color: " + COLOR_FONDO + ";");

        areaCalculo = new TextArea("Calcula una ruta para ver el desglose de la distancia.");
        areaCalculo.setEditable(false);
        areaCalculo.setWrapText(true);
        areaCalculo.setPrefRowCount(4);
        areaCalculo.setStyle(
            "-fx-control-inner-background: " + COLOR_PANEL + ";" +
            "-fx-text-fill: " + COLOR_TEXTO + ";" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: transparent;"
        );
        VBox contenedorCalculo = new VBox(areaCalculo);
        contenedorCalculo.setPadding(new javafx.geometry.Insets(0, 16, 16, 16));
        contenedorCalculo.setStyle("-fx-background-color: " + COLOR_FONDO + ";");

        BorderPane panelPrincipal = new BorderPane();
        panelPrincipal.setTop(crearControles());
        panelPrincipal.setCenter(centro);
        panelPrincipal.setBottom(contenedorCalculo);
        panelPrincipal.setStyle("-fx-background-color: " + COLOR_FONDO + ";");

        dibujar2D();

        Scene escena = new Scene(panelPrincipal, 1440, 830);
        stage.setTitle("Enrutador de Rutas Minimas en Campus");
        stage.setScene(escena);
        stage.show();
    }

    private void inicializarDatos() {
        agregarEdificio("Biblioteca", -300, -200, 140);
        agregarEdificio("Rectoria", -50, -320, 100);
        agregarEdificio("Cafeteria", 200, -250, 160);
        agregarEdificio("Administracion", 420, -100, 120);
        agregarEdificio("Auditorio", -380, 80, 90);
        agregarEdificio("Laboratorio", -100, 40, 180);
        agregarEdificio("Gimnasio", 180, 60, 110);
        agregarEdificio("Enfermeria", 400, 150, 100);
        agregarEdificio("Cancha", -200, 280, 40, TipoLugar.CANCHA);
        agregarEdificio("Parqueo", 100, 300, 35, TipoLugar.PARQUEO);

        agregarConexion("Biblioteca", "Rectoria", 120);
        agregarConexion("Rectoria", "Cafeteria", 100);
        agregarConexion("Cafeteria", "Administracion", 90);
        agregarConexion("Biblioteca", "Auditorio", 60);
        agregarConexion("Rectoria", "Laboratorio", 70);
        agregarConexion("Cafeteria", "Gimnasio", 65);
        agregarConexion("Administracion", "Enfermeria", 55);
        agregarConexion("Auditorio", "Laboratorio", 80);
        agregarConexion("Laboratorio", "Gimnasio", 75);
        agregarConexion("Gimnasio", "Enfermeria", 60);
        agregarConexion("Auditorio", "Cancha", 90);
        agregarConexion("Laboratorio", "Cancha", 100);
        agregarConexion("Gimnasio", "Parqueo", 85);
        agregarConexion("Enfermeria", "Parqueo", 70);
        agregarConexion("Cancha", "Parqueo", 120);
    }

    private void inicializarPosiciones2D() {
        posiciones2D.put("Biblioteca", new Posicion2D(10, 20));
        posiciones2D.put("Rectoria", new Posicion2D(150, 20));
        posiciones2D.put("Cafeteria", new Posicion2D(290, 20));
        posiciones2D.put("Administracion", new Posicion2D(430, 20));
        posiciones2D.put("Auditorio", new Posicion2D(10, 150));
        posiciones2D.put("Laboratorio", new Posicion2D(150, 150));
        posiciones2D.put("Gimnasio", new Posicion2D(290, 150));
        posiciones2D.put("Enfermeria", new Posicion2D(430, 150));
        posiciones2D.put("Cancha", new Posicion2D(80, 280));
        posiciones2D.put("Parqueo", new Posicion2D(360, 280));
    }

    private void agregarEdificio(String nombre, double x, double z, double alto) {
        agregarEdificio(nombre, x, z, alto, TipoLugar.EDIFICIO);
    }

    private void agregarEdificio(String nombre, double x, double z, double alto, TipoLugar tipo) {
        edificios.put(nombre, new Edificio(nombre, x, z, alto, tipo));
        adyacencia.put(nombre, new ArrayList<>());
    }

    private void agregarConexion(String origen, String destino, int distancia) {
        Conexion c = new Conexion(origen, destino, distancia);
        conexiones.add(c);
        adyacencia.get(origen).add(new String[]{destino, String.valueOf(distancia)});
        adyacencia.get(destino).add(new String[]{origen, String.valueOf(distancia)});
    }

    private Box crearPiso() {
        Box piso = new Box(1200, 10, 900);
        piso.setMaterial(new PhongMaterial(Color.web("#262b26")));
        return piso;
    }

    private Color colorNormalDe(Edificio e) {
        switch (e.tipo) {
            case CANCHA: return COLOR_CANCHA;
            case PARQUEO: return COLOR_PARQUEO;
            default: return COLOR_EDIFICIO;
        }
    }

    private Group crearEdificio(Edificio e) {
        colorNormalPorEdificio.put(e.nombre, colorNormalDe(e));
        switch (e.tipo) {
            case CANCHA: return crearCancha(e);
            case PARQUEO: return crearParqueo(e);
            default: return crearEdificioNormal(e);
        }
    }

    private Text crearEtiqueta(Edificio e) {
        Text etiqueta = new Text(e.nombre);
        etiqueta.setFont(Font.font(14));
        etiqueta.setFill(Color.WHITE);
        etiqueta.setTranslateX(e.x - (e.nombre.length() * 3.5));
        etiqueta.setTranslateZ(e.z);
        etiqueta.setTranslateY(PISO_Y - e.alto - 12);
        return etiqueta;
    }

    private Group crearEdificioNormal(Edificio e) {
        Box caja = new Box(60, e.alto, 60);
        caja.setTranslateX(e.x);
        caja.setTranslateZ(e.z);
        caja.setTranslateY(PISO_Y - e.alto / 2.0);
        caja.setMaterial(new PhongMaterial(colorNormalDe(e)));
        cajasPorEdificio.put(e.nombre, caja);

        return new Group(caja, crearEtiqueta(e));
    }

    // Cancha: superficie plana verde con borde, linea central y circulo central,
    // en vez de una caja alta como los edificios.
    private Group crearCancha(Edificio e) {
        double ancho = 130, profundo = 80, grosor = 4;

        Box piso = new Box(ancho, grosor, profundo);
        piso.setTranslateX(e.x);
        piso.setTranslateZ(e.z);
        piso.setTranslateY(PISO_Y - grosor / 2.0);
        piso.setMaterial(new PhongMaterial(colorNormalDe(e)));
        cajasPorEdificio.put(e.nombre, piso);

        double yLinea = PISO_Y - grosor - 0.5;
        double grosorLinea = 2;
        PhongMaterial blanco = new PhongMaterial(Color.WHITE);

        Box bordeSuperior = new Box(ancho, 1, grosorLinea);
        bordeSuperior.setMaterial(blanco);
        bordeSuperior.setTranslateX(e.x);
        bordeSuperior.setTranslateZ(e.z - profundo / 2.0);
        bordeSuperior.setTranslateY(yLinea);

        Box bordeInferior = new Box(ancho, 1, grosorLinea);
        bordeInferior.setMaterial(blanco);
        bordeInferior.setTranslateX(e.x);
        bordeInferior.setTranslateZ(e.z + profundo / 2.0);
        bordeInferior.setTranslateY(yLinea);

        Box bordeIzquierdo = new Box(grosorLinea, 1, profundo);
        bordeIzquierdo.setMaterial(blanco);
        bordeIzquierdo.setTranslateX(e.x - ancho / 2.0);
        bordeIzquierdo.setTranslateZ(e.z);
        bordeIzquierdo.setTranslateY(yLinea);

        Box bordeDerecho = new Box(grosorLinea, 1, profundo);
        bordeDerecho.setMaterial(blanco);
        bordeDerecho.setTranslateX(e.x + ancho / 2.0);
        bordeDerecho.setTranslateZ(e.z);
        bordeDerecho.setTranslateY(yLinea);

        Box lineaCentral = new Box(grosorLinea, 1, profundo);
        lineaCentral.setMaterial(blanco);
        lineaCentral.setTranslateX(e.x);
        lineaCentral.setTranslateZ(e.z);
        lineaCentral.setTranslateY(yLinea);

        Cylinder circuloCentral = new Cylinder(14, 1);
        circuloCentral.setMaterial(blanco);
        circuloCentral.setTranslateX(e.x);
        circuloCentral.setTranslateZ(e.z);
        circuloCentral.setTranslateY(yLinea);

        return new Group(piso, bordeSuperior, bordeInferior, bordeIzquierdo, bordeDerecho,
                lineaCentral, circuloCentral, crearEtiqueta(e));
    }

    // Parqueo: superficie plana oscura con lineas blancas de espacios,
    // en vez de una caja alta como los edificios.
    private Group crearParqueo(Edificio e) {
        double ancho = 110, profundo = 70, grosor = 4;

        Box piso = new Box(ancho, grosor, profundo);
        piso.setTranslateX(e.x);
        piso.setTranslateZ(e.z);
        piso.setTranslateY(PISO_Y - grosor / 2.0);
        piso.setMaterial(new PhongMaterial(colorNormalDe(e)));
        cajasPorEdificio.put(e.nombre, piso);

        double yLinea = PISO_Y - grosor - 0.5;
        Group lineas = new Group();
        int numLineas = 4;
        double espaciado = ancho / (numLineas + 1.0);

        for (int i = 1; i <= numLineas; i++) {
            double offsetX = -ancho / 2.0 + espaciado * i;
            Box linea = new Box(1.5, 1, profundo * 0.8);
            linea.setMaterial(new PhongMaterial(Color.web("#e8e8e8")));
            linea.setTranslateX(e.x + offsetX);
            linea.setTranslateZ(e.z);
            linea.setTranslateY(yLinea);
            lineas.getChildren().add(linea);
        }

        return new Group(piso, lineas, crearEtiqueta(e));
    }

    private Cylinder crearCamino(Edificio origen, Edificio destino) {
        Point3D p1 = new Point3D(origen.x, PISO_Y - 1, origen.z);
        Point3D p2 = new Point3D(destino.x, PISO_Y - 1, destino.z);

        Point3D diferencia = p2.subtract(p1);
        double longitud = diferencia.magnitude();
        Point3D ejeY = new Point3D(0, 1, 0);
        Point3D ejeRotacion = diferencia.crossProduct(ejeY);
        double angulo = Math.acos(diferencia.normalize().dotProduct(ejeY));

        Cylinder camino = new Cylinder(2.5, longitud);
        camino.setMaterial(new PhongMaterial(COLOR_CAMINO));

        Point3D medio = p1.midpoint(p2);
        camino.setTranslateX(medio.getX());
        camino.setTranslateY(medio.getY());
        camino.setTranslateZ(medio.getZ());
        camino.getTransforms().add(new Rotate(-Math.toDegrees(angulo), ejeRotacion));

        return camino;
    }

    private void dibujar2D() {
        GraphicsContext gc = canvas2D.getGraphicsContext2D();
        gc.setFill(Color.web(COLOR_PANEL));
        gc.fillRect(0, 0, canvas2D.getWidth(), canvas2D.getHeight());
        gc.setFont(Font.font(12));

        gc.setTextAlign(TextAlignment.LEFT);
        for (Conexion c : conexiones) {
            Posicion2D p1 = posiciones2D.get(c.origen);
            Posicion2D p2 = posiciones2D.get(c.destino);
            double cx1 = p1.x + CAJA_2D_ANCHO / 2.0, cy1 = p1.y + CAJA_2D_ALTO / 2.0;
            double cx2 = p2.x + CAJA_2D_ANCHO / 2.0, cy2 = p2.y + CAJA_2D_ALTO / 2.0;

            boolean enRuta = esArcoResaltado(c.origen, c.destino);
            gc.setStroke(enRuta ? COLOR_CAMINO_RUTA : COLOR_CAMINO);
            gc.setLineWidth(enRuta ? 3 : 1.5);
            gc.strokeLine(cx1, cy1, cx2, cy2);

            double mx = (cx1 + cx2) / 2;
            double my = (cy1 + cy2) / 2;
            gc.setFill(enRuta ? Color.web("#a8f0d8") : Color.web("#b7bbc4"));
            gc.fillText(c.distancia + " m", mx, my);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        for (Edificio e : edificios.values()) {
            Posicion2D p = posiciones2D.get(e.nombre);
            boolean enRuta = rutaResaltada.contains(e.nombre);

            gc.setFill(enRuta ? COLOR_EDIFICIO_RUTA : colorNormalDe(e));
            gc.fillRoundRect(p.x, p.y, CAJA_2D_ANCHO, CAJA_2D_ALTO, 12, 12);
            gc.setStroke(Color.web("#3a3d44"));
            gc.setLineWidth(1);
            gc.strokeRoundRect(p.x, p.y, CAJA_2D_ANCHO, CAJA_2D_ALTO, 12, 12);

            gc.setFill(Color.WHITE);
            gc.fillText(e.nombre, p.x + CAJA_2D_ANCHO / 2.0, p.y + CAJA_2D_ALTO / 2.0 + 4);
        }
    }

    private VBox crearControles() {
        String[] nombres = edificios.keySet().toArray(new String[0]);

        ComboBox<String> comboInicio = new ComboBox<>(FXCollections.observableArrayList(nombres));
        ComboBox<String> comboFin = new ComboBox<>(FXCollections.observableArrayList(nombres));
        comboInicio.setValue(nombres[0]);
        comboFin.setValue(nombres[nombres.length - 1]);
        comboInicio.setStyle("-fx-background-radius: 8;");
        comboFin.setStyle("-fx-background-radius: 8;");

        Button botonCalcular = new Button("Calcular ruta");
        botonCalcular.setStyle(
            "-fx-background-color: " + COLOR_ACENTO + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 8 20 8 20;"
        );
        botonCalcular.setOnAction(e -> calcularRuta(comboInicio.getValue(), comboFin.getValue()));

        Label lblInicio = new Label("Inicio:");
        Label lblFin = new Label("Destino:");
        lblInicio.setStyle("-fx-text-fill: " + COLOR_TEXTO + ";");
        lblFin.setStyle("-fx-text-fill: " + COLOR_TEXTO + ";");

        labelResultado = new Label("Selecciona inicio y destino para calcular la ruta.");
        labelResultado.setStyle("-fx-text-fill: " + COLOR_TEXTO + "; -fx-font-size: 12;");

        HBox fila = new HBox(12, lblInicio, comboInicio, lblFin, comboFin, botonCalcular);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        fila.setPadding(new javafx.geometry.Insets(16, 16, 6, 16));

        VBox contenedor = new VBox(4, fila, labelResultado);
        VBox.setMargin(labelResultado, new javafx.geometry.Insets(0, 16, 12, 16));
        contenedor.setStyle("-fx-background-color: " + COLOR_PANEL + ";");
        return contenedor;
    }

    private void calcularRuta(String inicio, String fin) {
        if (inicio == null || fin == null || inicio.equals(fin)) {
            labelResultado.setText("Elige un inicio y un destino distintos.");
            return;
        }

        Map<String, Integer> distancias = new HashMap<>();
        Map<String, String> previos = new HashMap<>();
        for (String nombre : edificios.keySet()) distancias.put(nombre, Integer.MAX_VALUE);
        distancias.put(inicio, 0);

        PriorityQueue<String> pendientes = new PriorityQueue<>(Comparator.comparingInt(distancias::get));
        pendientes.add(inicio);
        Set<String> visitados = new HashSet<>();

        while (!pendientes.isEmpty()) {
            String actual = pendientes.poll();
            if (visitados.contains(actual)) continue;
            visitados.add(actual);

            for (String[] vecino : adyacencia.get(actual)) {
                String nombreVecino = vecino[0];
                int peso = Integer.parseInt(vecino[1]);
                int nuevaDistancia = distancias.get(actual) + peso;
                if (nuevaDistancia < distancias.get(nombreVecino)) {
                    distancias.put(nombreVecino, nuevaDistancia);
                    previos.put(nombreVecino, actual);
                    pendientes.add(nombreVecino);
                }
            }
        }

        if (distancias.get(fin) == Integer.MAX_VALUE) {
            labelResultado.setText("No existe una ruta entre " + inicio + " y " + fin + ".");
            rutaResaltada = new ArrayList<>();
            areaCalculo.setText("No existe una ruta entre " + inicio + " y " + fin + ".");
        } else {
            List<String> ruta = new ArrayList<>();
            String paso = fin;
            while (paso != null) {
                ruta.add(0, paso);
                paso = previos.get(paso);
            }
            rutaResaltada = ruta;
            labelResultado.setText("Ruta: " + String.join(" -> ", ruta) + "   |   Total: " + distancias.get(fin) + " m");
            areaCalculo.setText(construirExplicacionCalculo(ruta, inicio, fin));
        }

        actualizarColores();
        dibujar2D();
    }

    private String construirExplicacionCalculo(List<String> ruta, String inicio, String fin) {
        StringBuilder sb = new StringBuilder();
        sb.append("Como se calculo la ruta de ").append(inicio).append(" a ").append(fin).append(":\n\n");

        int acumulado = 0;
        for (int i = 0; i < ruta.size() - 1; i++) {
            String a = ruta.get(i);
            String b = ruta.get(i + 1);
            int d = obtenerDistancia(a, b);
            acumulado += d;
            sb.append(a).append(" -> ").append(b).append(":  ").append(d)
              .append(" m   (acumulado: ").append(acumulado).append(" m)\n");
        }

        sb.append("\nDistancia total: ").append(acumulado).append(" m\n\n");
        sb.append("Es la ruta mas corta porque Dijkstra revisa, edificio por edificio, ")
          .append("todas las conexiones alcanzables y siempre se queda con la combinacion ")
          .append("que acumula la menor distancia total hasta llegar a ").append(fin).append(".");
        return sb.toString();
    }

    private int obtenerDistancia(String a, String b) {
        for (Conexion c : conexiones) {
            if ((c.origen.equals(a) && c.destino.equals(b)) || (c.origen.equals(b) && c.destino.equals(a))) {
                return c.distancia;
            }
        }
        return 0;
    }

    private void actualizarColores() {
        for (Map.Entry<String, Box> entrada : cajasPorEdificio.entrySet()) {
            boolean enRuta = rutaResaltada.contains(entrada.getKey());
            Color normal = colorNormalPorEdificio.get(entrada.getKey());
            entrada.getValue().setMaterial(new PhongMaterial(enRuta ? COLOR_EDIFICIO_RUTA : normal));
        }
        for (Map.Entry<Conexion, Cylinder> entrada : cilindrosPorConexion.entrySet()) {
            Conexion c = entrada.getKey();
            boolean enRuta = esArcoResaltado(c.origen, c.destino);
            entrada.getValue().setMaterial(new PhongMaterial(enRuta ? COLOR_CAMINO_RUTA : COLOR_CAMINO));
        }
    }

    private boolean esArcoResaltado(String a, String b) {
        for (int i = 0; i < rutaResaltada.size() - 1; i++) {
            String x = rutaResaltada.get(i);
            String y = rutaResaltada.get(i + 1);
            if ((x.equals(a) && y.equals(b)) || (x.equals(b) && y.equals(a))) return true;
        }
        return false;
    }

    private void habilitarRotacionConMouse(SubScene subScene) {
        subScene.setOnMousePressed(e -> {
            anclaX = e.getSceneX();
            anclaY = e.getSceneY();
            anguloAnclaX = rotateX.getAngle();
            anguloAnclaY = rotateY.getAngle();
        });
        subScene.setOnMouseDragged(e -> {
            rotateX.setAngle(anguloAnclaX - (anclaY - e.getSceneY()));
            rotateY.setAngle(anguloAnclaY + (anclaX - e.getSceneX()));
        });
    }

    private void habilitarZoomConRueda(SubScene subScene) {
        subScene.setOnScroll(e -> camera.setTranslateZ(camera.getTranslateZ() + e.getDeltaY()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
