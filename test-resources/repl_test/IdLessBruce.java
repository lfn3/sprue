package net.lfn3.sprue;

import java.lang.Object;
import java.lang.String;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("net.lfn3.sprue.java_output.clj")
public class IdLessBruce extends Batman implements Dated {
  private final LocalDate effectiveDate;

  private final boolean inEffect;

  private final String username;

  @Nullable
  private final BigDecimal aField;

  public IdLessBruce(LocalDate effectiveDate, boolean inEffect, String username,
      @Nullable BigDecimal aField) {
    super(effectiveDate, inEffect, username, aField);
    this.effectiveDate = effectiveDate;
    this.inEffect = inEffect;
    this.username = username;
    this.aField = aField;
  }

  public LocalDate getEffectiveDate() {
    return this.effectiveDate;
  }

  public boolean isInEffect() {
    return this.inEffect;
  }

  public String getUsername() {
    return this.username;
  }

  @Nullable
  public BigDecimal getAField() {
    return this.aField;
  }

  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (this == o) return true;
    IdLessBruce that = (IdLessBruce) o;
    return super.equals(that) &&
    	Objects.equals(this.effectiveDate, that.effectiveDate) &&
    	Objects.equals(this.inEffect, that.inEffect) &&
    	Objects.equals(this.username, that.username) &&
    	Objects.equals(this.aField, that.aField);
  }

  public int hashcode() {
    return Objects.hash(effectiveDate, inEffect, username, aField);
  }

  public String toString() {
    return "IdLessBruce{" +
    	effectiveDate + ", " +
    	inEffect + ", " +
    	username + ", " +
    	aField + '}' +
    	super.toString();
  }
}
