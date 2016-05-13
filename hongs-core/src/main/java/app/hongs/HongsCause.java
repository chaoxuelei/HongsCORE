package app.hongs;

/**
 * 异常基类
 * @author Hongs
 */
public interface HongsCause
{

    public int getErrno();

    public String getError();

    public Throwable getCause();

    public String getMessage( );

    public String getLocalizedMessage();

    public String getLocalizedSection();

    public String[] getLocalizedOptions();

    public HongsCause setLocalizedSection(String lang);

    public HongsCause setLocalizedOptions(String... opts);

}
