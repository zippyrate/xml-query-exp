package org.xmlquery;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Take a list of {@link XPathExpression}s and generate a {@link XPathResultValueTable} with a column
 * for each expression. Expressions may be relative, in which case they get their context from their
 * closest leftmost node.
 *
 * @see XPathExpression
 * @see XPathResultValueTable
 * @see XPathResultValue
 */
public class XPathExpressionProcessor
{
  private SimpleShortKeyGenerator expressionKeyGenerator, valueKeyGenerator;

  public XPathExpressionProcessor()
  {
    this.expressionKeyGenerator = new SimpleShortKeyGenerator();
    this.valueKeyGenerator = new SimpleShortKeyGenerator();
  }

  public XPathResultValueTable processXPathExpressions(Document document, List<XPathExpression> expressions,
    XPathResultValueTable accumulatedXPathResultValueTable) throws XMLQueryException
  {
    XPathResultValueTable resultTable = new XPathResultValueTable(accumulatedXPathResultValueTable.getTableName(),
      accumulatedXPathResultValueTable.getColumnNames());

    for (XPathExpression expression : expressions)
      resultTable = generateXPathResultValueTable(document, expression, resultTable);

    return resultTable;
  }

  private XPathResultValueTable generateXPathResultValueTable(Document document, XPathExpression xPathExpression,
    XPathResultValueTable accumulatedXPathResultValueTable) throws XMLQueryException
  {
    XPathResultValueTable newAccumlatedResult = new XPathResultValueTable(
      accumulatedXPathResultValueTable.getTableName());
    newAccumlatedResult.setColumnNames(accumulatedXPathResultValueTable.getColumnNames());

    if (accumulatedXPathResultValueTable.isEmpty()) { // Build the rows in the first column
      List<XPathResultValue> firstColumn = generateResultValuesColumnForNode("/", document, xPathExpression);

      for (XPathResultValue cell : firstColumn) {
        List<XPathResultValue> newRow = new ArrayList<>();
        newRow.add(cell);
        newAccumlatedResult.addRow(newRow);
      }
    } else { // Expand additional columns
      for (List<XPathResultValue> currentRow : accumulatedXPathResultValueTable.getRows()) {
        //XPathGeneratedResultValue mostRecentResultValueWithAbsolutePathInRow = getMostRecentResultValueWithAbsolutePath(currentRow);
        XPathResultValue mostRecentResultValueWithAbsolutePathInRow = getMostRecentResultValue(currentRow);
        Object mostRecentAbsoluteNodeInRow = mostRecentResultValueWithAbsolutePathInRow.getNode();
        String xPathLocationOfMostRecentAbsoluteNodeInRow = mostRecentResultValueWithAbsolutePathInRow
          .getXPathAbsoluteLocation();
        List<XPathResultValue> resultValuesColumnForNode = generateResultValuesColumnForNode(
          xPathLocationOfMostRecentAbsoluteNodeInRow, mostRecentAbsoluteNodeInRow, xPathExpression);

        for (XPathResultValue resultValueForNode : resultValuesColumnForNode) {
          List<XPathResultValue> newRow = new ArrayList<>(currentRow);
          newRow.add(resultValueForNode);
          newAccumlatedResult.addRow(newRow);
        }
      }
    }

    return newAccumlatedResult;
  }

  private List<XPathResultValue> generateResultValuesColumnForNode(String contextXPathLocation, Object context,
    XPathExpression xPathExpression) throws XMLQueryException
  {
    List<XPathResultValue> generatedResultValues = new ArrayList<>();

    for (Object resultNode : XMLUtil.executeXPathExpression(context, xPathExpression.getXPathExpression())) {
      String value = node2StringValue(resultNode);
      String resultXPathLocation = XMLUtil.getAbsoluteXPathLocation(resultNode);
      XPathResultValue generatedResultValue;

      if (xPathExpression.isExpressionKey())
        generatedResultValue = new XPathResultValue(xPathExpression.getXPathExpression(), resultXPathLocation,
          resultNode, expressionKeyGenerator.getKey(xPathExpression.getSourceURI(), resultXPathLocation));
      else if (xPathExpression.isValueKey())
        generatedResultValue = new XPathResultValue(xPathExpression.getXPathExpression(), resultXPathLocation,
          resultNode, valueKeyGenerator.getKey(xPathExpression.getSourceURI(), value.toString()));
      else
        generatedResultValue = new XPathResultValue(xPathExpression.getXPathExpression(), resultXPathLocation,
          resultNode, value);

      generatedResultValues.add(generatedResultValue);
    }
    return generatedResultValues;
  }

  @SuppressWarnings("unused") private XPathResultValue getMostRecentResultValueWithAbsolutePath(
    List<XPathResultValue> resultValues) throws XMLQueryException
  {
    for (int i = resultValues.size(); i > 0; i--)
      if (resultValues.get(i - 1).wasGeneratedFromAbsolutePath())
        return resultValues.get(i - 1);

    throw new XMLQueryException("must be at least one absolute path in generator expressions - none found");
  }

  private XPathResultValue getMostRecentResultValue(List<XPathResultValue> resultValues) throws XMLQueryException
  {
    if (!resultValues.isEmpty())
      return resultValues.get(resultValues.size() - 1);
    else
      throw new XMLQueryException("must be at least one absolute path in generator expressions - none found");
  }

  private String node2StringValue(Object node) throws XMLQueryException
  {
    if (XMLUtil.isElementNode(node)) {
      Element element = (Element)node;
      String text = element.getTextTrim();
      return text;
    } else if (XMLUtil.isAttributeNode(node)) {
      Attribute attribute = (Attribute)node;
      String value = attribute.getValue();
      return value;
    } else
      throw new XMLQueryException("unsupported node type " + node.getClass());
  }

  private class SimpleShortKeyGenerator
  {
    private Map<String, String> keyMap;
    private Long currentKey;

    public SimpleShortKeyGenerator()
    {
      keyMap = new HashMap<>();
      currentKey = 0L;
    }

    public String getKey(String uri, String value) throws XMLQueryException
    {
      String compoundValue = uri + ":" + value;

      if (keyMap.containsKey(compoundValue))
        return keyMap.get(compoundValue);
      else {
        if (currentKey == Long.MAX_VALUE)
          throw new XMLQueryException("maximum number of keys generated");
        currentKey++;
        keyMap.put(compoundValue, currentKey.toString());
        return currentKey.toString();
      }
    }
  }
}
