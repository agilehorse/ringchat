package cz.cvut.fel.bulkodav.exceptions;

/**
 * The {@link OperationException} is raised when some operation fails.
 */
public class OperationException extends Exception
{
    private final String message;

    /**
     * The constructor for {@link OperationException} class.
     *
     * @param message The message.
     */
    public OperationException(String message)
    {
        this.message = message;
    }

    /**
     * Gets the exception message.
     *
     * @return the exception mesage.
     */
    @Override
    public String getMessage()
    {
        return message;
    }
}
