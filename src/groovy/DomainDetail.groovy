import org.codehaus.groovy.grails.commons.GrailsDomainClass


class DomainDetail extends ClassDetail {

  GrailsDomainClass gdc


  String getNodeName() {
    gdc?.simpleName ?: "No domain class defined"
  }


  List getSubClasses() {

  }

  List getInterestingProperties() {

  }

  List getInterestingMethods() {

  }

  List getInterestingAssociations() {

  }


}