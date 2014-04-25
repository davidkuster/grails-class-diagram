import org.codehaus.groovy.grails.commons.GrailsDomainClass


class DomainDetail extends ClassDetail {

  GrailsDomainClass gdc


  String getNodeName() {
    gdc?.simpleName ?: "No domain class defined"
  }


}