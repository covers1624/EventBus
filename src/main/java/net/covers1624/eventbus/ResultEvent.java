package net.covers1624.eventbus;

/**
 * Created by covers1624 on 10/4/21.
 */
public interface ResultEvent {

    Result getResult();

    void setResult(Result result);

    enum Result {
        DENY,
        DEFAULT,
        ALLOW,
    }
}
