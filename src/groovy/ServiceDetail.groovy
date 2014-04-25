import org.codehaus.groovy.grails.commons.GrailsServiceClass


class ServiceDetail extends ClassDetail {

  GrailsServiceClass gsc

  String getNodeName() {
    "${gcc}Service" - "Artefact > "
  }

}