/**
 * 
 */
package gui;

import java.math.BigDecimal;

/**
 * @author Detlef Tribius
 *
 */
public class Data implements Comparable<Data>
{
    
    /**
     * COUNTER_KEY = "counterKey" - Key zum Zugriff auf die Nummer/den Zaehler...
     */
    public final static String COUNTER_KEY = "counterKey";

    /**
     * DELTA_TIME_KEY = "deltaTimeKey"
     */
    public final static String DELTA_TIME_KEY = "deltaTimeKey";

    /**
     * 
     */
    public final static String DISTANCE_KEY = "distanceKey";
    
    /**
     * Long counter - Zaehler
     */
    private final Long counter;
    
    /**
     * BigDecimal deltaTime - Laufzeit/Dauer
     */
    private final BigDecimal deltaTime;
    
    /**
     * 
     */
    private final BigDecimal distance;
    
    /**
     * Data() - Defaultkonstruktor
     */
    public Data()
    {
        this(Long.valueOf(0L), BigDecimal.ZERO, BigDecimal.ZERO);
    }
    
    /**
     * Data(Long counter, BigDecimal deltaTime, BigDecimal distance) - Konstruktor aus allen Attributen...
     * @param counter
     * @param duration
     * @param distance
     */
    public Data(Long counter, BigDecimal deltaTime, BigDecimal distance)
    {
        this.counter = (counter != null)? counter : Long.valueOf(0L);
        this.deltaTime = (deltaTime != null)? deltaTime : BigDecimal.ZERO;
        this.distance = (distance != null)? distance : BigDecimal.ZERO;
    }
    
    /**
     * getKeys() - liefert Array mit allen Keys...
     * @return String[]
     */
    public String[] getKeys()
    {
        return new String[] {Data.COUNTER_KEY,
                             Data.DELTA_TIME_KEY,
                             Data.DISTANCE_KEY};
    }    
    
    /**
     * getValue(String key) - Bereitstellung der Anzeige...
     * @param key
     * @return String-Anzeige
     */
    public final String getValue(String key)
    {
        if (Data.COUNTER_KEY.equals(key))
        {
            return (this.counter != null)? this.counter.toString() : null;
        }
        if (Data.DELTA_TIME_KEY.equals(key))
        {
            return (this.deltaTime != null)? this.deltaTime.toString() : null;
        }
        if (Data.DISTANCE_KEY.equals(key))
        {
            return (this.distance != null)? this.distance.toString() : null;
        }
        return null;
    }
        
    @Override
    public int compareTo(Data another)
    {
        return this.counter.compareTo(another.counter);
    }

    /**
     * toString() - zu Protokollzwecken... (z.B. Logging)
     */
    @Override
    public String toString()
    {
        return new StringBuilder().append("[")
                                  .append(this.counter)
                                  .append(" ")
                                  .append(this.deltaTime)
                                  .append(" ")
                                  .append(this.distance)
                                  .append("]")
                                  .toString();
    }
}
