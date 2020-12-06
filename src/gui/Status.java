/**
 * 
 */
package gui;

/**
 * @author Detlef Tribius
 *
 */
public enum Status
{
    /**
     * Started("Started")
     */
    Started("Started"),
    /**
     * Stopped("Stopped")
     */
    Stopped("Stopped"),
    /**
     * Finish("Finish")
     */
    Finish("Finish");

    /**
     * private Status(String status) - Privater Konstruktor...
     * @param status
     */
    private Status(String status)
    {
        this.status = status;
    }

    /**
     * String status - Kennung fuer den Status...   
     */
    private final String status;

    /**
     * @return the status
     */
    public final String getStatus()
    {
        return status;
    }
    
}
