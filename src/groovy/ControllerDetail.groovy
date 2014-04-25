import org.codehaus.groovy.grails.commons.GrailsControllerClass


class ControllerDetail extends ClassDetail {

  GrailsControllerClass gcc

  String getNodeName() {
    "${gcc}Controller" - "Artefact > "
  }

}