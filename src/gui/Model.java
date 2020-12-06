package gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import raspi.hardware.US100Sensor;

// Vgl. https://www.baeldung.com/java-observer-pattern
// auch https://wiki.swechsler.de/doku.php?id=java:allgemein:mvc-beispiel
// http://www.nullpointer.at/2011/02/06/howto-gui-mit-swing-teil-4-interaktion-mit-der-gui/
// http://www.javaquizplayer.com/blogposts/java-propertychangelistener-as-observer-19.html
// TableModel...
// Vgl.: https://examples.javacodegeeks.com/core-java/java-swing-mvc-example/
/**
 * 
 * Das Model haelt die Zustandsgroessen..
 *
 * 
 * 
 */
public class Model 
{
    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(Model.class);
    
    /**
     * Kennung isRaspi kennzeichnet, der Lauf erfolgt auf dem RasberryPi.
     * Die Kennung wird zur Laufzeit aus den Systemvariablen fuer das
     * Betriebssystem und die Architektur ermittelt. Mit dieser Kennung kann
     * die Beauftragung von Raspi-internen Programmen gesteuert werden.
     */
    private final boolean isRaspi;
    /**
     * OS_NAME_RASPI = "linux" - Kennung fuer Linux.
     * <p>
     * ...wird verwendet, um einen Raspi zu erkennen...
     * </p>
     */
    public final static String OS_NAME_RASPI = "linux";
    /**
     * OS_ARCH_RASPI = "arm" - Kennung fuer die ARM-Architektur.
     * <p>
     * ...wird verwendet, um einen Raspi zu erkennen...
     * </p>
     */
    public final static String OS_ARCH_RASPI = "arm";
    
    /**
     * Status status - Zustandsgroesse Status
     */
    private static Status status = Status.Stopped;
    
    /**
     * counter - Taktzaehler (keine weitere funktionale Bedeutung)
     */
    private static long counter = 0L;
    
    /**
     * 
     */
    private static BigDecimal deltaTime = BigDecimal.ZERO;
    
    /**
     * 
     */
    private static BigDecimal distance = BigDecimal.ZERO;
    
    /**
     * Taktung in ms
     */
    public final static int CYCLE_TIME = 1000;
    
    /**
     * Die Steuerung instanziieren...
     * <p>
     * Der ControlThread ermoeglicht die  regelmaessige Beauftragung von
     * Algorithmen (Regelungen)...
     * </p>
     * <p>
     * Die Servo- und Motor-Aktivitaeten werden zusatzlich an die User-Interaktion
     * beauftragt.
     * </p>
     */
    private ControlThread controlThread = new ControlThread(CYCLE_TIME);
    
    /**
     * Referenz auf den GPIO-controller...
     * <p>
     * Der GPIO-Controller bedient die GPIO-Schnittstelle des Raspi.
     * </p>
     */
    private final GpioController gpioController;
    
    
    private final US100Sensor us100Sensor;
    
    /**
     * gpioPinOutputMap nimmt die GpioPinDigitalOutput-Objekte auf, 
     * Key ist dabei jeweils der Pin_Name, z.B. "GPIO 21"...
     * <p>
     * Verwendung: Unter dem Key 'Name des GPIO' wird die Referenz auf den Pin abgelegt. 
     * </p>
     */
    private final java.util.TreeMap<String, GpioPinDigitalOutput> gpioPinOutputMap = new java.util.TreeMap<>();
    
    /**
     * 
     */
    public final static Pin TRIG_TX_PIN = RaspiPin.GPIO_01;
    
    /**
     * 
     */
    public final static Pin ECHO_RX_PIN = RaspiPin.GPIO_02;
    
    /**
     * ...die folgenden Pins werden angesprochen...
     */
    private final static Pin[] GPIO_PINS = 
    {
        // RaspiPin.GPIO_00    // GPIO 17, Board-Nr=11
    };
    
    /**
     * PIN_NAMES - String-Array mit den Namen der RaspiPin's.
     * Das Array wird aus dem Array GPIO_PINS[] befuellt.
     */
    public final static String[] PIN_NAMES = new String[GPIO_PINS.length];
    
    static 
    {
        // Befuellen des Arrays PIN_NAMES[] aus GPIO_PINS[]...
        for(int index = 0; index < GPIO_PINS.length; index++)
        {
            PIN_NAMES[index] = GPIO_PINS[index].getName();
        }
    }
    
    /**
     * NAME_START_BUTTON = "StartButton"
     */
    public final static String NAME_START_BUTTON = "StartButton";
    /**
     * NAME_STOP_BUTTON = "StopButton"
     */
    public final static String NAME_STOP_BUTTON = "StopButton";
    /**
     * NAME_END_BUTTON = "EndButton"
     */
    public final static String NAME_END_BUTTON = "EndButton";
    
    /**
     * dataMap - nimmt die Eingaben auf...
     * <p>
     * Ablage key => Eingabe-Object
     * </p>
     */
    private final java.util.TreeMap<String, Object>  dataMap = new java.util.TreeMap<>();

    /**
     * Unter dem DATA_KEY werden Anzeigewerte fuer die Oberflaeche zusammengefasst.
     * Mit jedem Takt werden diese Anzeigewerte fuer die GUI bereitgestellt.
     */
    public final static String DATA_KEY = "dataKey"; 
    
    /**
     * Key "dataIsRunnableKey" => isRunnable
     */
    public final static String DATA_IS_RUNNABLE_KEY = "dataIsRunnableKey";
    
    /**
     * DATA_KEYS[] - Array mit ergaenzenden Keys zur zusaetzlichen Ablage in der
     * dataMap...
     */
    private final static String[] DATA_KEYS = 
    {
        Model.DATA_KEY,                         // => Data-Objekt
        Model.DATA_IS_RUNNABLE_KEY              // => isRunnable-Flag (TRUE/FALSE)
    };
    
    /**
     * support - Referenz auf den PropertyChangeSupport...
     */
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    /**
     * Default-Konstruktor 
     */
    public Model()
    {
        // 1.) Wo erfolgt der Lauf, auf einem Raspi?
        final String os_name = System.getProperty("os.name").toLowerCase();
        final String os_arch = System.getProperty("os.arch").toLowerCase();
        logger.debug("Betriebssytem: " + os_name + " " + os_arch);
        // Kennung isRaspi setzen...
        this.isRaspi = OS_NAME_RASPI.equals(os_name) && OS_ARCH_RASPI.equals(os_arch);
        
        // ...den gpioController anlegen...
        this.gpioController = isRaspi? GpioFactory.getInstance() : null;
        
        this.us100Sensor = new US100Sensor(this.gpioController, 
                                           Model.TRIG_TX_PIN, 
                                           Model.ECHO_RX_PIN);
        
        // *** Befuellen der dataMap... ***
        // Die dataMap muss mit allen Key-Eintraegen befuellt werden, sonst 
        // ist setProperty(String key, Object newValue) unwirksam!
        for (String key: Model.DATA_KEYS)
        {
            this.dataMap.put(key, null);
        }
        
        // Wenn Output, dann wird jeder Pin entsprechend konfiguriert
        // und ein Boolsche Wert (Datenhaltung) zugeordnet...
        for (Pin pin: Model.GPIO_PINS)
        {
            final String key = pin.getName();
            this.dataMap.put(key, Boolean.FALSE);
            logger.debug(key + " in dataMap aufgenommen.");
            if (isRaspi)
            {
                // Zugriff auf die Pin nur wenn Lauf auf dem Raspi...
                GpioPinDigitalOutput gpioPin = this.gpioController.provisionDigitalOutputPin(pin, key, PinState.LOW);
                gpioPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
                this.gpioPinOutputMap.put(key, gpioPin);
            } 
            else
            {
                // Der Lauf erfolgt nicht auf dem Raspi...
                this.gpioPinOutputMap.put(key, null);
            }
        }
    
        this.dataMap.put(Model.DATA_IS_RUNNABLE_KEY, Boolean.TRUE);
    }

    
    /**
     * 
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.support.addPropertyChangeListener(listener);
    }

    /**
     * 
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        this.support.removePropertyChangeListener(listener);
    }

    /**
     * setProperty(String key, Object newValue) - Die View wird informiert...
     * 
     * @param key
     * @param newValue
     */
    public void setProperty(String key, Object newValue)
    {
        if (this.dataMap.containsKey(key))
        {
            Object oldValue = this.dataMap.get(key); 
            this.dataMap.put(key, newValue);
            
            if (oldValue == null || newValue == null || !oldValue.equals(newValue))
            {
                logger.debug(key + ": " + oldValue + " => " + newValue);
            }
            
            support.firePropertyChange(key, oldValue, newValue);
        }
    }

    /**
     * @throws InterruptedException 
     * 
     */
    private synchronized void doSensor() throws InterruptedException
    {
        this.us100Sensor.startMeasuring();
    }
    
    /**
     * notifyGUI()
     */
    public synchronized void notifyGUI()
    {
        if (this.dataMap.containsKey(Model.DATA_KEY))
        {
            Model.deltaTime = this.us100Sensor.getDeltaTime();
            Model.distance = this.us100Sensor.getDistance();
            
            final Data data = new Data(Long.valueOf(Model.counter),
                                       Model.deltaTime,
                                       Model.distance);
            setProperty(Model.DATA_KEY, data);
        }
    }
    
    /**
     * start() - Methode wird durch den Start-Button beauftragt
     */
    public synchronized void start()
    {
        if (Model.status != Status.Started)
        {
            this.controlThread.start();
            setStatus(Status.Started);
            setCounter(0);
            logger.debug("Started()...");
            setProperty(DATA_IS_RUNNABLE_KEY, Boolean.FALSE);
        }    
    }
    
    /**
     * stop() - Methode wird durch den Stop-Button beauftragt
     */
    public synchronized void stop()
    {
        this.controlThread.stop();
        setStatus(Status.Stopped);
        // Offensichtlich kann ein neuer Thread eher gestartet werden,
        // als der alte beendet wurde. Daher verzoegern wir die 
        // Moeglichkeit des Neustartes ein wenig...
        // => Evtl. spaetere Ueberarbeitung notwendig! 
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException exception){}
        logger.debug("Stopped()...");
        setProperty(DATA_IS_RUNNABLE_KEY, Boolean.TRUE);
    }
    
    /**
     * shutdown()...
     * <p>
     * Der gpioController wird auf dem Raspi heruntergefahren...
     * </p>
     */
    public void shutdown()
    {
       logger.debug("shutdown()..."); 
       if (isRaspi)
       {
           this.gpioController.shutdown();  
       }
    }
    
    @Override
    public String toString()
    {
        return "gui.Model";
    }

    /**
     * setStatus(Status status)
     * 
     * @param status
     */
    public synchronized void setStatus(Status status)
    {
        Model.status = status;
    }

    /**
     * setCounter(int counter)
     * @param counter
     */
    public synchronized void setCounter(int counter)
    {
        Model.counter = counter;
    }
    
    /**
     * incrementCounter()
     */
    public synchronized void incrementCounter()
    {
        Model.counter++;
    }
    
    /**
     * ControlThread - Klasse zur Taktung der Aktionen... 
     *
     */
    class ControlThread implements Runnable
    {
        /**
         * 
         */
        private Thread worker;
        /**
         * isRunning - Flag...
         */
        private final AtomicBoolean isRunning = new AtomicBoolean(false); 
        
        /**
         * cycleTime - Zykluszeit in ms.
         */
        private final int cycleTime;
        
        /**
         * ControlThread(int cycleTime) - Konstruktor mit Zykluszeit in ms.
         * @param cycleTime - Zykluszeit (ms)
         */
        public ControlThread(int cycleTime)
        {
            this.cycleTime = cycleTime;
        }
        
        /**
         * 
         */
        public void start()
        {
            this.worker = new Thread(this);
            this.worker.start();
        }
        
        
        public void stop()
        {
            this.isRunning.set(false);
        }
        
        @Override
        public void run()
        {
            logger.debug("run()...");
            
            this.isRunning.set(true);
            
            while(this.isRunning.get())
            {
                doIt();
                try
                {
                    Thread.sleep(cycleTime);
                }
                catch(InterruptedException exception)
                {
                    Thread.currentThread().interrupt();
                    logger.error("Thread was interrupted, Failed to complete operation", exception);
                }
            }
        }
        
        /**
         * doIt()
         */
        private void doIt()
        {
            // incrementCounter() erhoeht den counter um 1...
            incrementCounter();
            
            try
            {
                doSensor();
            }
            catch(Exception exception)
            {
                logger.error("Exception in doSensor()", exception);
                Thread.currentThread().interrupt();
            }
            
            // ...die relevanten Daten werden in die GUI uebertragen...
            notifyGUI();
        }
    }

}
