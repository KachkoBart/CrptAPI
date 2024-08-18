package org.example;

import com.google.common.util.concurrent.RateLimiter;
import com.google.errorprone.annotations.ThreadSafe;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.http.client.HttpResponseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
public class CrptApi {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(CrptApi.class);
    private static final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Semaphore semaphore;
    private final long timeInMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        timeInMillis = timeUnit.toMillis(1);
    }

    public void runRequest(Document document, String signature) {
        log.debug("Requesting document: {}", document);
        try {
            semaphore.acquire();
            upSemaphore();
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
        String docJson = getDocJson(document, signature).toString();
        httpsRequest(docJson);
    }
    private void upSemaphore() {
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                semaphore.release();
            }
        };
        Timer timer = new Timer("Timer");
        timer.schedule(repeatedTask, timeInMillis);
    }

    private JSONObject getDocJson(Document document, String signature) {
        log.debug("Start make json from Document: {}", document);
        JSONObject doc = new JSONObject();
        doc.put("doc_id", document.getDocId());
        doc.put("doc_status", document.getDocStatus());
        doc.put("doc_type", document.getDocType());
        doc.put("owner_inn", document.getOwnerInn());
        doc.put("participant_inn", document.getParticipantInn());
        doc.put("producer_inn", document.getProducerInn());
        doc.put("production_date", document.getProductionDate());
        doc.put("production_type", document.getProductionType());
        if (document.getDescription() != null) {
            JSONObject inn = new JSONObject();
            inn.put("participantInn", document.getParticipantInnDescription());
            doc.put("description", inn);
        }
        if (document.getImportRequest() != null) {
            doc.put("importRequest", document.getImportRequest());
        }

        if (document.getProducts() != null) {
            JSONArray productsList = new JSONArray();
            for (Product product : document.getProducts()) {
                JSONObject products = new JSONObject();
                products.put("owner_inn", product.getOwnerInn());
                products.put("producer_inn", product.getProducerInn());
                products.put("production_date", product.getProductionDate());
                products.put("tnved_code", product.getTnvedCode());
                products.put("certificate_document", product.getCertificateDocument());
                products.put("certificate_document_date", product.getCertificateDocumentDate());
                products.put("certificate_document_number", product.getCertificateDocumentNumber());
                products.put("uit_code", product.getUitCode());
                products.put("uitu_code", product.getUituCode());
                productsList.add(products);
                doc.put("products", productsList);
            }
        }
        doc.put("reg_date", document.getRegDate());
        doc.put("reg_number", document.getRegNumber());
        log.debug("End make json from Document: {} \n Answer: {}", document, doc.toJSONString());
        return doc;
    }

    private void httpsRequest(String json) {
        try {
            URL myurl = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        } catch (Exception ex) {
            log.error("Error in httpsRequest {}", ex.getMessage());
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    private class Request{
        private Document document;
        private String signature;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public class Document {
        private String description;
        private String participantInnDescription;
        private String docId;
        private String docStatus;
        private String docType;
        private Boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private String regDate;
        private String regNumber;
        private List<Product> products;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }
}


