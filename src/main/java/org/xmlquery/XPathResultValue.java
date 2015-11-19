package org.xmlquery;

/**
 * A value generated after evaluating an XPath expression. Includes the XPATH expression, the
 * XPATH absolute location of the node, the result value and the source JDOM node (which
 * contains the JDOM node that contained the value).
 *
 * @see XPathResultValueTable
 */
public class XPathResultValue
{
  private final String value;
  private final String xPathAbsoluteLocation;
  private final String xPathExpression;
  private final Object node;

  public XPathResultValue(String xPathExpression, String xPathAbsoluteLocation, Object node, String value)
  {
    this.value = value;
    this.xPathAbsoluteLocation = xPathAbsoluteLocation;
    this.xPathExpression = xPathExpression;
    this.node = node;
  }

  public String getValue() { return this.value; }

  public Object getNode()
  {
    return node;
  }

  public String getXPathExpression()
  {
    return xPathExpression;
  }

  public String getXPathAbsoluteLocation()
  {
    return xPathAbsoluteLocation;
  }

  public boolean wasGeneratedFromAbsolutePath()
  {
    return xPathExpression.startsWith("/");
  }

  public String toString()
  {
    return "[location: " + xPathAbsoluteLocation + ", query: " + xPathExpression + ", node: " + node + ", " + super
      .toString() + "]";
  }
}
