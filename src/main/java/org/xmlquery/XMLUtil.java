package org.xmlquery;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that provides some simple XML utility methods.
 */
public class XMLUtil
{
  /**
   * Method that writes an XML file from an instance of a Document.
   */
  public static void generateXMLFile(Document doc, String outputXMLFileName) throws XMLQueryException
  {
    OutputStream xmlStream = createOutputXMLStream(outputXMLFileName);

    if ((doc == null) || !doc.hasRootElement())
      throw new XMLQueryException("document is empty");

    try {
      XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());
      serializer.output(doc, xmlStream);
    } catch (IOException e) {
      throw new XMLQueryException("error writing XML file '" + outputXMLFileName + "': " + e.getMessage());
    } finally {
      try {
        xmlStream.close();
      } catch (IOException e) {
        // TODO log
      }
    }
  }

  /**
   * Method that returns XML string representing an instance of a Document.
   */
  public static String generateXMLString(Document doc) throws XMLQueryException
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    if ((doc == null) || !doc.hasRootElement())
      throw new XMLQueryException("document is empty");

    try {
      XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());
      serializer.output(doc, outputStream);
    } catch (IOException e) {
      throw new XMLQueryException("error writing XML string: " + e.getMessage());
    }

    return outputStream.toString();
  }

  /**
   * Method that reads a simple XML file and generates an instance of a Document from it.
   */
  public static Document processXMLStream(String inputXMLStreamName) throws XMLQueryException
  {
    Document doc;
    
    try {
      SAXBuilder builder = new SAXBuilder();
      InputStream xmlStream = createInputXMLStream(inputXMLStreamName);
      doc = builder.build(xmlStream);
      xmlStream.close();
    } catch (Exception e) {
      throw new XMLQueryException("error opening XML file '" + inputXMLStreamName + "': " + e.getMessage());
    }

    return doc;
  }

  /**
   * Get a list of files in a directory with the extension ".xml".
   */
  public static Set<String> getXMLFileNames(String directoryName) throws XMLQueryException
  {
    URI directoryURI = getURI(directoryName);
    File dir = new File(directoryURI.getPath());
    Set<String> result = new HashSet<>();
    XMLFileFilter xmlFileFilter = new XMLFileFilter();
    String[] xmlFileNames = dir.list(xmlFileFilter);

    if (xmlFileNames == null)
      throw new XMLQueryException("invalid directory " + directoryName);
    else
      for (String xmlFileName : xmlFileNames)
        result.add(prependDirectoryName(directoryName, xmlFileName));
    return result;
  }

  public static void builtXPath2ElementAndAttributeMap(String currentXPath, Element currentElement,
    Map<String, Element> xPath2ElementMap, Map<String, Attribute> xPath2AttributeMap) throws XMLQueryException
  {
    for (Attribute attribute : getAttributes(currentElement)) {
      String xPathAbsoluteLocation = currentXPath + "@" + attribute.getQualifiedName();
      xPath2AttributeMap.put(xPathAbsoluteLocation, attribute);
    }

    int currentIndex = 1;
    for (Element subElement : getSubElements(currentElement)) {
      String xPathAbsoluteLocation = currentXPath + "[" + currentIndex++ + "]/" + subElement.getQualifiedName();
      xPath2ElementMap.put(xPathAbsoluteLocation, subElement);
    }
  }

  // TODO: rough and ready
  public static String getAbsoluteXPathLocation(Object node) throws XMLQueryException
  {
    if (isElementNode(node)) {
      Element element = (Element)node;
      return buildAbsoluteXPath(element);
    } else if (isAttributeNode(node)) {
      Attribute attribute = (Attribute)node;
      return buildAbsoluteXPath(attribute);
    } else
      throw new XMLQueryException(
        "only elements and attributes currently supported for absolute path construction");
  }

  private static String buildAbsoluteXPath(Element element)
  {
    if (element.isRootElement())
      return "/" + element.getQualifiedName();
    else {
      int indexOf = getXPathIndexOfChild(element);
      return buildAbsoluteXPath(element.getParentElement()) + "/" + element.getQualifiedName() + "[" + indexOf + "]";
    }
  }

  private static int getXPathIndexOfChild(Element element)
  {
    Element parent = element.getParentElement();
    return parent.getChildren(element.getName()).indexOf(element) + 1;
  }

  private static String buildAbsoluteXPath(Attribute attribute)
  {
    Element parent = attribute.getParent();
    return buildAbsoluteXPath(parent) + "@" + attribute.getQualifiedName();
  }

  private static URI getURI(String path) throws XMLQueryException
  {
    try {
      return new URI(path);
    } catch (URISyntaxException e) {
      throw new XMLQueryException("URI exception processing path " + path + ":" + e.getMessage());
    }
  }

  /**
   * Method that reads an XML string and generates an instance of a Document from it.
   */
  public static Document processXMLString(String inputXMLString) throws XMLQueryException
  {
    try {
      SAXBuilder builder = new SAXBuilder();
      return builder.build(new StringReader(inputXMLString));
    } catch (Exception e) {
      throw new XMLQueryException("error processing XML string: " + e.getMessage());
    }
  }

  public static List<Object> executeXPathExpression(Object context, String xPathExpression) throws XMLQueryException
  {
    try {
      XPath xPath = XPath.newInstance(xPathExpression);
      List<Object> results = new ArrayList<Object>();

      for (Object result : xPath.selectNodes(context)) {
        results.add(result);
      }

      return results;
    } catch (JDOMException e) {
      throw new XMLQueryException("JDOM exception processing " + xPathExpression + ": " + e.getMessage());
    }
  }

  public static Element createElement(Document doc, Element parentElement, String elementName)
  {
    Element element = new Element(elementName);

    if (parentElement == null)
      doc.setRootElement(element);
    else
      parentElement.addContent(element);

    return element;
  }

  public static void setAttribute(Element element, String attributeName, String attributeValue, String namespacePrefix,
    String namespaceURI)
  {
    Attribute attribute = new Attribute(attributeName, attributeValue,
      Namespace.getNamespace(namespacePrefix, namespaceURI));

    element.setAttribute(attribute);
  }

  @SuppressWarnings("unchecked") public static List<Attribute> getAttributes(Element element)
  {
    return new ArrayList<Attribute>(element.getAttributes());
  }

  public static List<Element> getSubElements(Element element)
  {
    List<Element> result = new ArrayList<Element>();

    for (Object o : element.getChildren())
      if (o instanceof Element)
        result.add((Element)o);

    return result;
  }

  public static boolean isSchema(Element element)
  {
    return hasName(element, "schema");
  }

  public static boolean isElementNode(Object node)
  {
    return node instanceof Element;
  }

  public static boolean isAttributeNode(Object node)
  {
    return node instanceof Attribute;
  }

  public static boolean isAll(Element element)
  {
    return hasName(element, "all");
  }

  public static boolean isComplexType(Element element)
  {
    return hasName(element, "complexType");
  }

  public static boolean isSequence(Element element)
  {
    return hasName(element, "sequence");
  }

  public static boolean isGroup(Element element)
  {
    return hasName(element, "group");
  }

  public static boolean isAttributeGroup(Element element)
  {
    return hasName(element, "attributeGroup");
  }

  public static boolean isChoice(Element element)
  {
    return hasName(element, "choice");
  }

  public static boolean isAny(Element element)
  {
    return hasName(element, "any");
  }

  public static boolean isAnyAttribute(Element element)
  {
    return hasName(element, "anyAttribute");
  }

  public static boolean isAttribute(Element element)
  {
    return hasName(element, "attribute");
  }

  public static boolean isComplexContent(Element element)
  {
    return hasName(element, "complexContent");
  }

  public static boolean isSimpleContent(Element element)
  {
    return hasName(element, "simpleContent");
  }

  public static boolean isSimpleContext(Element element)
  {
    return hasName(element, "simpleContext");
  }

  public static boolean isSimpleType(Element element)
  {
    return hasName(element, "simpleType");
  }

  public static boolean isRefElement(Element element)
  {
    return hasRefAttribute(element);
  }

  public static String getNameAttribute(Element element) throws XMLQueryException
  {
    return getNameAttributeValue(element);
  }

  public static String getReafAttribute(Element element) throws XMLQueryException
  {
    return getRefAttributeValue(element);
  }

  public static String getTypeAttribute(Element element) throws XMLQueryException
  {
    return getTypeAttributeValue(element);
  }

  public static String getUseAttribute(Element element) throws XMLQueryException
  {
    return getUseAttributeValue(element);
  }

  public static String getMinOccursAttribute(Element element) throws XMLQueryException
  {
    return getMinOccursAttributeValue(element);
  }

  public static String getMaxOccursAttribute(Element element) throws XMLQueryException
  {
    return getMaxOccursAttributeValue(element);
  }

  public static boolean hasNameAttribute(Element element)
  {
    return hasAttribute(element, "name");
  }

  public static boolean hasValueAttribute(Element element)
  {
    return hasAttribute(element, "value");
  }

  public static boolean hasBaseAttribute(Element element)
  {
    return hasAttribute(element, "base");
  }

  public static boolean hasMixedAttribute(Element element)
  {
    return hasAttribute(element, "mixed");
  }

  public static boolean hasTypeAttribute(Element element)
  {
    return hasAttribute(element, "type");
  }

  public static boolean hasDefaultAttribute(Element element)
  {
    return hasAttribute(element, "default");
  }

  public static boolean hasFixedAttribute(Element element)
  {
    return hasAttribute(element, "fixed");
  }

  public static boolean hasRefAttribute(Element element)
  {
    return hasAttribute(element, "ref");
  }

  public static boolean hasUseAttribute(Element element)
  {
    return hasAttribute(element, "use");
  }

  public static boolean hasMaxOccursAttribute(Element element)
  {
    return hasAttribute(element, "maxOccurs");
  }

  public static boolean hasMinOccursAttribute(Element element)
  {
    return hasAttribute(element, "minOccurs");
  }

  public static String getNameAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "name");
  }

  public static String getValueAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "value");
  }

  public static String getMixedAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "mixed");
  }

  public static String getBaseAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "base");
  }

  public static String getTypeAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "type");
  }

  public static String getDefaultAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "default");
  }

  public static String getFixedAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "fixed");
  }

  public static String getRefAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "ref");
  }

  public static String getUseAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "use");
  }

  public static String getMaxOccursAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "maxOccurs");
  }

  public static String getMinOccursAttributeValue(Element element) throws XMLQueryException
  {
    return getAttributeValue(element, "minOccurs");
  }

  public static Element getComplexTypeChild(Element element) throws XMLQueryException
  {
    if (!hasComplexTypeChild(element))
      throw new XMLQueryException("expecting complexType child for element " + getNameAttributeValue(element));
    return element.getChild("complexType", element.getNamespace());
  }

  public static String getAttributeValue(Element element, String attributeName) throws XMLQueryException
  {
    if (!hasAttribute(element, attributeName))
      throw new XMLQueryException("no " + attributeName + " attribute found in element " + element.getName());

    return element.getAttributeValue(attributeName);
  }

  public static Element getFirstChild(Element element) throws XMLQueryException
  {
    if (element.getChildren() == null)
      throw new XMLQueryException(
        "getFirstChild called on non-parent element " + getNameAttributeValue(element));

    return (Element)element.getChildren().get(0);
  }

  public static boolean hasChildren(Element element)
  {
    return element.getChildren() != null;
  }

  public static boolean hasComplexTypeChild(Element element)
  {
    return element.getChild("complexType", element.getNamespace()) != null;
  }

  public static boolean hasSimpleTypeChild(Element element)
  {
    return element.getChild("simpleTypeChild", element.getNamespace()) != null;
  }

  private static boolean hasName(Element element, String name)
  {
    return element.getName() != null && element.getName().equals(name);
  }

  private static boolean hasAttribute(Element element, String attributeName)
  {
    return element.getAttributeValue(attributeName) != null;
  }

  private static String prependDirectoryName(String directoryName, String fileName)
  {
    if (directoryName.endsWith(File.separator)) {
      if (fileName.startsWith(File.separator))
        return directoryName + fileName.substring(1); // Will be at least one character
      else
        return directoryName + fileName;
    } else
      return directoryName + File.separator + fileName;
  }

  private static OutputStream createOutputXMLStream(String outputXMLStreamName) throws XMLQueryException
  {
    try {
      return new FileOutputStream(outputXMLStreamName);
    } catch (IOException e) {
      throw new XMLQueryException(
        "error creating XML serializer for XML stream '" + outputXMLStreamName + "': " + e.getMessage());
    }
  }

  private static InputStream createInputXMLStream(String inputXMLStreamName) throws XMLQueryException
  {
    InputStream xmlStream = null;

    try {
      URL url = new URL(inputXMLStreamName);
      String protocol = url.getProtocol();
      if (protocol.equals("file")) {
        String path = url.getPath();
        xmlStream = new FileInputStream(path);
      } else
        xmlStream = url.openStream();
    } catch (MalformedURLException e) {
      throw new XMLQueryException("invalid URL for XML stream '" + inputXMLStreamName + "': " + e.getMessage());
    } catch (IOException e) {
      throw new XMLQueryException(
        "IO error opening XML stream '" + inputXMLStreamName + "': " + e.getMessage());
    }

    return xmlStream;
  }

  private static class XMLFileFilter implements FilenameFilter
  {
    public boolean accept(File dir, String name)
    {
      return name.endsWith(".xml");
    }
  }
}
