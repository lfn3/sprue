package net.lfn3.sprue;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.lang.Object;
import java.lang.String;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("net.lfn3.sprue.java_output.clj")
public class Bruce extends Batman implements Dated {
  public static final String BRUCE_ID_SERIALIZER_FIELD_NAME = "lfn3_bruce_id";

  public static final String BRUCE_LOG_ID_SERIALIZER_FIELD_NAME = "lfn3_bruce_log_id";

  @JsonProperty(Bruce.BRUCE_ID_SERIALIZER_FIELD_NAME)
  @ApiModelProperty(
      dataType = "long"
  )
  private final BruceId id;

  @JsonProperty(Bruce.BRUCE_LOG_ID_SERIALIZER_FIELD_NAME)
  @ApiModelProperty(
      dataType = "long"
  )
  private final BruceLogId logId;

  private final LocalDate effectiveDate;

  private final boolean inEffect;

  private final String username;

  @Nullable
  private final BigDecimal aField;

  public Bruce(BruceId id, BruceLogId logId, LocalDate effectiveDate, boolean inEffect,
      String username, BigDecimal aField) {
    super(id, logId, effectiveDate, inEffect, username, aField);
    this.id = id;
    this.logId = logId;
    this.effectiveDate = effectiveDate;
    this.inEffect = inEffect;
    this.username = username;
    this.aField = aField;
  }

  public BruceId getId() {
    return this.id;
  }

  public BruceLogId getLogId() {
    return this.logId;
  }

  public LocalDate getEffectiveDate() {
    return this.effectiveDate;
  }

  public boolean getInEffect() {
    return this.inEffect;
  }

  public String getUsername() {
    return this.username;
  }

  public BigDecimal getAField() {
    return this.aField;
  }

  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (this == o) return true;
    Bruce that = (Bruce) o;
    return super.equals(that) &&
    	Objects.equals(this.id, that.id) &&
    	Objects.equals(this.logId, that.logId) &&
    	Objects.equals(this.effectiveDate, that.effectiveDate) &&
    	Objects.equals(this.inEffect, that.inEffect) &&
    	Objects.equals(this.username, that.username) &&
    	Objects.equals(this.aField, that.aField);
  }

  public int hashcode() {
    return Objects.hash(id, logId, effectiveDate, inEffect, username, aField);
  }

  public String toString() {
    return "Bruce{" +
    	Object.toString(id) + ", " +
    	Object.toString(logId) + ", " +
    	Object.toString(effectiveDate) + ", " +
    	Object.toString(inEffect) + ", " +
    	Object.toString(username) + ", " +
    	Object.toString(aField) + "}" +
    	super.toString();
  }
}
