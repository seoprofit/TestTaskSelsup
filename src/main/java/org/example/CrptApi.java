package org.example;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final TimeUnit TIMEUNIT;
    private final int REQUEST_LIMIT;
    private JSONSerialize jsonSerialize;
    private DataProvider dataProvider;
    private List<Timestamp> timestampRequests;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        TIMEUNIT = timeUnit;
        REQUEST_LIMIT = requestLimit;
        init();
    }

    private void init() {
        dataProvider = new DataProvider();
        jsonSerialize = new JSONSerialize();
        timestampRequests = new LinkedList<>();
    }


    public void createDocument(DocumentGoods documentGoods, String sign) throws IOException, InterruptedException {
        synchronized (CrptApi.class) {
            checkQueryLimits();
            dataProvider.sendData(sign, jsonSerialize.serialize(documentGoods));
            addQueryRecord();
        }
    }

    public void checkQueryLimits() {
        deleteExpiredRecords();

        if (timestampRequests.size() >= REQUEST_LIMIT) {
            throw new RuntimeException("Limit is over");
        }
    }

    public void deleteExpiredRecords() {
        Timestamp timeLimit = new Timestamp(System.currentTimeMillis() - getCurrentTime(TIMEUNIT));
        ListIterator<Timestamp> iterator = timestampRequests.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().before(timeLimit)) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    public long getCurrentTime(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case SECONDS -> 1_0_00L;
            case MINUTES -> 60_000L;
            case HOURS -> 360_000L;
            case DAYS -> 8_640_000L;
            default -> 1000L;
        };
    }

    public void addQueryRecord() {
        timestampRequests.add(new Timestamp(System.currentTimeMillis()));
    }


    @JsonAutoDetect
    @Setter
    @Getter
    @Builder
    public static class DocumentGoods {
        private Description description;
        private String docId;
        private String docStatus;
        private DocType docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDateTime productionDate;
        private String productionType;
        private List<Products> products;
        private LocalDateTime regDate;
        private String regNumber;
    }

    public enum DocType {
        LP_INTRODUCE_GOODS, LP_INTRODUCE_GOODS_CSV, LP_INTRODUCE_GOODS_XML
    }

    @Setter
    @Getter
    @Builder
    public static class Description {
        private String participantInn;
    }

    @Setter
    @Getter
    @Builder
    public static class Products {
        private String certificate_document;
        private LocalDateTime certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private LocalDateTime production_date;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }

    public static class DataProvider {
        private HttpClient httpClient;
        private final String REQUEST_URI = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        public DataProvider() {
            init();
        }

        private void init() {
            httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
        }

        public void sendData(String sign, String json) throws IOException, InterruptedException {
            HttpRequest request = createRequest(sign, json);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            checkStatus(response.statusCode());
        }

        private HttpRequest createRequest(String sign, String json) {
           return HttpRequest.newBuilder()
                    .uri(URI.create(REQUEST_URI))
                    .setHeader("sign", sign)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
        }

        private void checkStatus(int statusCode) {
            if (statusCode <= 299 && statusCode >= 200) {
                System.out.println("OK");
            } else System.out.println("Response code is: " + statusCode);

        }
    }


    public static class JSONSerialize {
        private ObjectMapper mapper;

        public JSONSerialize() {
            init();
        }

        private void init() {
            mapper = new ObjectMapper();
        }


        public String serialize(Object object) throws IOException {
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, object);
            return writer.toString();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);
        String sign = "SIGN";
        crptApi.createDocument(DocumentGoods.builder()
                .docId("12")
                .docType(DocType.LP_INTRODUCE_GOODS)
                .build(), sign);
    }
}
