package com.selfxdsd.core.contracts.invoices;

import com.selfxdsd.api.*;
import com.selfxdsd.api.storage.Storage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * An Invoice stored in self.
 * @author criske
 * @version $Id$
 * @since 0.0.3
 * @checkstyle ExecutableStatementCount (500 lines)
 * @checkstyle TrailingComment (500 lines)
 * @todo #826:60min Modify the PDF template and the code in toPdf()
 *  such that more tasks are written on more pages. At the moment
 *  only 40 tasks are written to the 1-page PDF.
 */
public final class StoredInvoice implements Invoice {

    /**
     * Invoice id.
     */
    private final int id;

    /**
     * Contract.
     */
    private final Contract contract;

    /**
     * Creation time.
     */
    private final LocalDateTime createdAt;

    /**
     * Time when this Invoice has been paid.
     */
    private final LocalDateTime paymentTime;

    /**
     * The payment's transaction ID.
     */
    private final String transactionId;

    /**
     * Who emitted this Invoice?
     */
    private final String billedBy;

    /**
     * To whom is this Invoice billed? Who pays?
     */
    private final String billedTo;

    /**
     * Self storage context.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param id Invoice id.
     * @param contract Contract.
     * @param createdAt Invoice creation time.
     * @param paymentTime Time when this Invoice has been paid.
     * @param transactionId The payment's transaction ID.
     * @param billedBy Who emitted the Invoice.
     * @param billedTo Who pays it.
     * @param storage Self storage context.
     */
    public StoredInvoice(
        final int id,
        final Contract contract,
        final LocalDateTime createdAt,
        final LocalDateTime paymentTime,
        final String transactionId,
        final String billedBy,
        final String billedTo,
        final Storage storage
    ) {
        this.id = id;
        this.contract = contract;
        this.createdAt = createdAt;
        this.paymentTime = paymentTime;
        this.transactionId = transactionId;
        this.billedBy = billedBy;
        this.billedTo = billedTo;
        this.storage = storage;
    }

    @Override
    public int invoiceId() {
        return this.id;
    }

    @Override
    public InvoicedTask register(
        final Task task,
        final BigDecimal commission
    ) {
        final Contract.Id taskContract = new Contract.Id(
            task.project().repoFullName(),
            task.assignee().username(),
            task.project().provider(),
            task.role()
        );
        if(!this.contract.contractId().equals(taskContract)) {
            throw new IllegalArgumentException(
                "The given Task does not belong to this Invoice!"
            );
        } else {
            if(this.isPaid()) {
                throw new IllegalStateException(
                    "Invoice is already paid, can't add a new Task to it!"
                );
            }
            return this.storage.invoicedTasks().register(
                this, task, commission
            );
        }
    }

    @Override
    public Contract contract() {
        return this.contract;
    }

    @Override
    public LocalDateTime createdAt() {
        return this.createdAt;
    }

    @Override
    public LocalDateTime paymentTime() {
        return this.paymentTime;
    }

    @Override
    public String transactionId() {
        return this.transactionId;
    }

    @Override
    public String billedBy() {
        final String billedBy;
        if(this.billedBy != null && !this.billedBy.isEmpty()) {
            billedBy = this.billedBy;
        } else {
            billedBy = this.contract.contributor().billingInfo().toString();
        }
        return billedBy;
    }

    @Override
    public String billedTo() {
        final String billedTo;
        if(this.billedTo != null && !this.billedTo.isEmpty()) {
            billedTo = this.billedTo;
        } else {
            billedTo = this.contract.project().billingInfo().toString();
        }
        return billedTo;
    }

    @Override
    public InvoicedTasks tasks() {
        return this.storage.invoicedTasks().ofInvoice(this);
    }

    @Override
    public boolean isPaid() {
        return this.paymentTime != null && this.transactionId != null;
    }

    @Override
    public PlatformInvoice platformInvoice() {
        final PlatformInvoice found;
        if(this.isPaid()) {
            if(this.transactionId.startsWith("fake_payment_")) {
                found = null;
            } else {
                found = this.storage.platformInvoices().getByPayment(
                    this.transactionId, this.paymentTime
                );
            }
        } else {
            found = null;
        }
        return found;
    }

    @Override
    public void toPdf(final OutputStream out) throws IOException {
        final PDDocument doc = PDDocument.load(
            this.getResourceAsFile("invoice_template.pdf")
        );
        final PDDocumentCatalog docCatalog = doc.getDocumentCatalog();
        final PDAcroForm acroForm = docCatalog.getAcroForm();

        acroForm.getField("invoiceId").setValue("SLFX-" + this.id);
        acroForm.getField("createdAt").setValue(
            this.createdAt.toLocalDate().toString()
        );
        acroForm.getField("billedBy").setValue(this.billedBy());
        acroForm.getField("billedTo").setValue(this.billedTo());
        acroForm.getField("project").setValue(
            this.contract.project().repoFullName()
        );
        acroForm.getField("role").setValue(
            this.contract.role()
        );
        acroForm.getField("hourlyRate").setValue(
            NumberFormat
                .getCurrencyInstance(Locale.GERMANY)
                .format(
                    this.contract.hourlyRate()
                        .divide(BigDecimal.valueOf(100))
                )
        );

        acroForm.getField("totalDue").setValue(
            NumberFormat
                .getCurrencyInstance(Locale.GERMANY)
                .format(
                    this.totalAmount()
                        .divide(BigDecimal.valueOf(100))
                )
        );
        if(this.isPaid()) {
            acroForm.getField("status").setValue("Paid");
        } else {
            acroForm.getField("status").setValue("Active (not paid)");
        }
        final StringBuilder taskIds = new StringBuilder();
        final StringBuilder estimations = new StringBuilder();
        final StringBuilder values = new StringBuilder();
        final StringBuilder commissions = new StringBuilder();

        int count = 0;
        for(final InvoicedTask invoiced : this.tasks()) {
            if(count == 40) {
                taskIds.append("...");
                estimations.append("...");
                values.append("...");
                commissions.append("...");
                break;
            }
            final Task task = invoiced.task();
            taskIds.append(task.issueId()).append("\n");
            estimations.append(task.estimation()).append("\n");
            values.append(invoiced.value().divide(BigDecimal.valueOf(100)))
                .append("\n");
            commissions.append(
                invoiced.commission().divide(BigDecimal.valueOf(100))
            ).append("\n");
            count++;
        }

        acroForm.getField("taskIds").setValue(taskIds.toString());
        acroForm.getField("estimations").setValue(estimations.toString());
        acroForm.getField("values").setValue(values.toString());
        acroForm.getField("commissions").setValue(commissions.toString());

        acroForm.flatten();

        doc.addPage(docCatalog.getPages().get(0));
        doc.removePage(1); //remove trailing blank page

        doc.save(out);
        doc.close();
    }

    @Override
    public BigDecimal totalAmount() {
        BigDecimal total = BigDecimal.valueOf(0);
        for(final InvoicedTask task : this.tasks()) {
            total = total.add(task.totalAmount());
        }
        return total;
    }

    @Override
    public BigDecimal amount() {
        BigDecimal revenue = BigDecimal.valueOf(0);
        for (final InvoicedTask task : this.tasks()) {
            revenue = revenue.add(task.value());
        }
        return revenue;
    }

    @Override
    public BigDecimal commission() {
        BigDecimal commission = BigDecimal.valueOf(0);
        for(final InvoicedTask task : this.tasks()) {
            commission = commission.add(task.commission());
        }
        return commission;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Invoice
            && this.id == ((Invoice) obj).invoiceId());
    }

    /**
     * Convenience method to get the PDF template resource as a File.
     * @param resourcePath Name of the file.
     * @throws IOException If something goes wrong.
     * @return File.
     */
    private File getResourceAsFile(
        final String resourcePath
    ) throws IOException {
        final InputStream stream = this.getClass().getClassLoader()
            .getResourceAsStream(resourcePath);
        final File tempFile = File.createTempFile(
            String.valueOf(stream.hashCode()), ".tmp"
        );
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}
