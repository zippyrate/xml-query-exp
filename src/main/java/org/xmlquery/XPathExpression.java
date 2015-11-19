package org.xmlquery;

/**
 * @see XPathExpressionProcessor
 */
public class XPathExpression
{
  private final boolean isExpressionKey;
  private final boolean isValueKey;
  private final String sourceName;
  private final String xPathExpression;
  private final String displayName;

  public XPathExpression(String sourceName, String xPathExpression, String displayName)
  {
    this.isExpressionKey = false;
    this.isValueKey = false;
    this.sourceName = sourceName;
    this.xPathExpression = xPathExpression.trim();
    this.displayName = displayName;
  }

  public XPathExpression(String sourceName, String xPathExpression, String displayName,
    boolean isExpressionKey)
  {
    this.isExpressionKey = isExpressionKey;
    this.isValueKey = false;
    this.sourceName = sourceName;
    this.xPathExpression = xPathExpression.trim();
    this.displayName = displayName;
  }

  public XPathExpression(String sourceName, String xPathExpression, String displayName,
    boolean isExpressionKey, boolean isValueKey) throws XMLQueryException
  {
    this.isExpressionKey = isExpressionKey;
    this.isValueKey = isValueKey;
    this.sourceName = sourceName;
    this.xPathExpression = xPathExpression.trim();
    this.displayName = displayName;
  }

  public boolean isAbsoluteExpression()
  {
    return xPathExpression.startsWith(("/"));
  }

  public boolean isRelativeExpression()
  {
    return !isAbsoluteExpression();
  }

  public boolean isExpressionKey()
  {
    return isExpressionKey;
  }

  public boolean isValueKey()
  {
    return isValueKey;
  }

  public String getSourceURI()
  {
    return sourceName;
  }

  public String getXPathExpression()
  {
    return xPathExpression;
  }

  public String getDisplayName()
  {
    return displayName;
  }


  @Override public String toString()
  {
    return "XPathResultValueGeneratorExpression{" +
      "isExpressionKey=" + isExpressionKey +
      ", isValueKey=" + isValueKey +
      ", sourceName='" + sourceName + '\'' +
      ", xPathExpression='" + xPathExpression + '\'' +
      ", displayName='" + displayName + '\'' +
      '}';
  }
}
