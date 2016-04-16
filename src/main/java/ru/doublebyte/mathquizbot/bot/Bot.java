package ru.doublebyte.mathquizbot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import ru.doublebyte.mathquizbot.bot.types.response.GetUpdatesResponse;
import ru.doublebyte.mathquizbot.bot.types.Update;

import java.util.*;

public abstract class Bot {

    private static Logger logger = LoggerFactory.getLogger(Bot.class);

    private String token;
    private String apiUrl;

    private RestTemplate restTemplate = new RestTemplate();

    private long lastId = 0;
    private int timeout = 0;
    private int updateLimit = 100;

    public Bot(String apiUrl, String token) {
        this.apiUrl = apiUrl;
        this.token = token;
    }

    protected abstract void processUpdate(Update update);

    /**
     * Fetch and process updates from Telegram bot API server
     */
    public void processUpdates() {
        logger.info("Fetching updates...");

        List<Update> updates = getUpdates(lastId, updateLimit, timeout);

        lastId = updates.stream()
                .map(Update::getId)
                .max(Long::compareTo)
                .orElse(lastId-1) + 1;

        updates.parallelStream().forEach(this::processUpdate);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Construct URL for method by name
     * @param method Method name
     * @return Url with bot token and method name
     */
    private String getMethodUrl(String method) {
        return apiUrl + token + "/" + method;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get incoming updates
     * https://core.telegram.org/bots/api#getupdates
     *
     * @param offset id of first update
     * @param limit Number of updates returned
     * @param timeout Timeout for long polling
     * @return Updates
     */
    protected List<Update> getUpdates(long offset, int limit, int timeout) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("offset", offset);
            params.put("limit", limit);
            params.put("timeout", timeout);

            GetUpdatesResponse response = restTemplate.getForObject(
                    getMethodUrl("getUpdates"), GetUpdatesResponse.class, params);

            if(response == null) {
                logger.warn("getUpdates null result");
                return new ArrayList<>();
            }

            if(!response.isOk()) {
                logger.warn("getUpdates error: {} - {}", response.getErrorCode(), response.getDescription());
                return new ArrayList<>();
            }

            if(response.getResult() == null) {
                return new ArrayList<>();
            }

            return response.getResult();
        } catch(Exception e) {
            logger.error("getUpdates failed", e);
            return new ArrayList<>();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getToken() {
        return token;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getUpdateLimit() {
        return updateLimit;
    }

    public void setUpdateLimit(int updateLimit) {
        this.updateLimit = updateLimit;
    }
}
