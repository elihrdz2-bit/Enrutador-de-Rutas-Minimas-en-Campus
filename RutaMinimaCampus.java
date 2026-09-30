



import javax.swing.*; //ES PARA CREAR LA VENTANA Y LOS BOTONES
import java.awt.*; // ES PARA PINTAR EL GRAFO Y LOS EDIFICIOS
import java.util.*; // ES PARA IMPORTAR LAS ESTRUCTURAS DE DATOS COMO MAP, LIST, SET, PRIORITYQUEUE
import java.util.List; // IMPORTAR ESPECIFICAMENTE LA CLASE LIST

public class RutaMinimaCampus extends JFrame {
                              //Aquí se declaran las clases de apoyo (Edificio y Conexión)
    static class Edificio {
        String nombre;
        int x, y;
        Edificio(String nombre, int x, int y) { this.nombre = nombre; this.x = x; this.y = y; }
    }

    static class Conexion {
        String origen, destino;
        int distancia;
        Conexion(String origen, String destino, int distancia) {
            this.origen = origen; this.destino = destino; this.distancia = distancia;
        }
    }
                                 //Aquí se declaran las variables donde se guarda todo el grafo
    private final Map<String, Edificio> edificios = new LinkedHashMap<>();
    private final List<Conexion> conexiones = new ArrayList<>();
    private final Map<String, List<String[]>> adyacencia = new HashMap<>(); // nombre -> [vecino, distancia]

    private List<String> rutaResaltada = new ArrayList<>();
    private int distanciaTotal = -1;

    private JComboBox<String> comboInicio;
    private JComboBox<String> comboFin;
    private JLabel labelResultado;
    private PanelGrafo panelGrafo;

    public RutaMinimaCampus() {
        super("ESTRUCTURA DE PRUEBA PARA EL PROYECTO");
        inicializarDatos();
        construirInterfaz();
    }
                                            //Aquí se determinan los edificios del campus
    private void inicializarDatos() {
        agregarEdificio("Biblioteca", 100, 60);
        agregarEdificio("Rectoria", 350, 60);
        agregarEdificio("Cafeteria", 600, 60);
        agregarEdificio("Administracion", 850, 60);
        agregarEdificio("Auditorio", 100, 260);
        agregarEdificio("Laboratorio", 350, 260);
        agregarEdificio("Gimnasio", 600, 260);
        agregarEdificio("Enfermeria", 850, 260);
        agregarEdificio("Cancha", 250, 460);
        agregarEdificio("Parqueo", 700, 460);
                                              //Aquí se establecen las distancias entre edificios
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
                                                    //Aquí se guardan los datos anteriores en las estructuras del grafo
    private void agregarEdificio(String nombre, int x, int y) {
        edificios.put(nombre, new Edificio(nombre, x, y));
        adyacencia.put(nombre, new ArrayList<>());
    }

    private void agregarConexion(String origen, String destino, int distancia) {
        conexiones.add(new Conexion(origen, destino, distancia));
        adyacencia.get(origen).add(new String[]{destino, String.valueOf(distancia)});
        adyacencia.get(destino).add(new String[]{origen, String.valueOf(distancia)});
    }
                                                //Aquí se crean los botones y menús de la ventana
    private void construirInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(25, 25, 25));

        JPanel panelControles = new JPanel();
        panelControles.setBackground(new Color(35, 35, 35));
        panelControles.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] nombres = edificios.keySet().toArray(new String[0]);
        comboInicio = new JComboBox<>(nombres);
        comboFin = new JComboBox<>(nombres);
        comboFin.setSelectedIndex(nombres.length - 1);

        JButton botonCalcular = new JButton("Calcular ruta mas corta");
        labelResultado = new JLabel("Selecciona un origen y un destino, luego presiona el boton.");
        labelResultado.setForeground(Color.WHITE);

        JLabel labelInicio = new JLabel("Inicio:");
        labelInicio.setForeground(Color.WHITE);
        JLabel labelFin = new JLabel("Destino:");
        labelFin.setForeground(Color.WHITE);

        panelControles.add(labelInicio);
        panelControles.add(comboInicio);
        panelControles.add(labelFin);
        panelControles.add(comboFin);
        panelControles.add(botonCalcular);

        botonCalcular.addActionListener(e -> calcularRuta());

        panelGrafo = new PanelGrafo();

        JScrollPane scroll = new JScrollPane(panelGrafo);
        scroll.getViewport().setBackground(new Color(25, 25, 25));

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBackground(new Color(35, 35, 35));
        panelInferior.add(labelResultado, BorderLayout.CENTER);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        add(panelControles, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        setSize(1020, 720);
        setLocationRelativeTo(null);
    }
                                            //Aquí se preparan las distancias iniciales antes de calcular
    private void calcularRuta() {
        String inicio = (String) comboInicio.getSelectedItem();
        String fin = (String) comboFin.getSelectedItem();

        if (inicio.equals(fin)) {
            labelResultado.setText("El punto de inicio y el destino no pueden ser el mismo.");
            rutaResaltada = new ArrayList<>();
            panelGrafo.repaint();
            return;
        }

        Map<String, Integer> distancias = new HashMap<>();
        Map<String, String> previos = new HashMap<>();
        for (String nombre : edificios.keySet()) distancias.put(nombre, Integer.MAX_VALUE);
        distancias.put(inicio, 0);

        PriorityQueue<String> pendientes = new PriorityQueue<>(Comparator.comparingInt(distancias::get));
        pendientes.add(inicio);
        Set<String> visitados = new HashSet<>();
                                                    //Aquí se calcula la ruta más corta (algoritmo de Dijkstra)
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
            distanciaTotal = -1;
        } else {                                          //Aquí se arma la lista de edificios de la ruta encontrada
            List<String> ruta = new ArrayList<>();
            String paso = fin;
            while (paso != null) {
                ruta.add(0, paso);
                paso = previos.get(paso);
            }
            rutaResaltada = ruta;
            distanciaTotal = distancias.get(fin);
            labelResultado.setText("Ruta mas corta: " + String.join(" -> ", ruta)
                    + "   |   Distancia total: " + distanciaTotal + " m");
        }
        panelGrafo.repaint();
    }
                                                                 //Aquí se revisa qué edificios y caminos van pintados de verde
    private boolean esArcoResaltado(String a, String b) {
        for (int i = 0; i < rutaResaltada.size() - 1; i++) {
            String x = rutaResaltada.get(i);
            String y = rutaResaltada.get(i + 1);
            if ((x.equals(a) && y.equals(b)) || (x.equals(b) && y.equals(a))) return true;
        }
        return false;
    }

    private boolean esNodoResaltado(String nombre) {
        return rutaResaltada.contains(nombre);
    }

    private class PanelGrafo extends JPanel {
        PanelGrafo() {
            setPreferredSize(new Dimension(1000, 560));
            setBackground(new Color(20, 20, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                                                    //Aquí se dibuja todo en la pantalla
            for (Conexion c : conexiones) {
                Edificio o = edificios.get(c.origen);
                Edificio d = edificios.get(c.destino);
                boolean resaltado = esArcoResaltado(c.origen, c.destino);

                g2.setColor(resaltado ? new Color(29, 158, 117) : new Color(90, 90, 90));
                g2.setStroke(new BasicStroke(resaltado ? 3f : 1.5f));
                g2.drawLine(o.x + 60, o.y + 20, d.x + 60, d.y + 20);

                int mx = (o.x + d.x) / 2 + 60;
                int my = (o.y + d.y) / 2 + 20;
                g2.setColor(resaltado ? new Color(120, 230, 190) : new Color(180, 180, 180));
                g2.drawString(c.distancia + " m", mx, my);
            }

            for (Edificio e : edificios.values()) {
                boolean resaltado = esNodoResaltado(e.nombre);
                g2.setColor(resaltado ? new Color(29, 158, 117) : new Color(60, 60, 60));
                g2.fillRoundRect(e.x, e.y, 120, 40, 12, 12);
                g2.setColor(resaltado ? Color.WHITE : new Color(210, 210, 210));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(e.x, e.y, 120, 40, 12, 12);

                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(e.nombre);
                g2.setColor(Color.WHITE);
                g2.drawString(e.nombre, e.x + (120 - textWidth) / 2, e.y + 25);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RutaMinimaCampus().setVisible(true));
    }
}