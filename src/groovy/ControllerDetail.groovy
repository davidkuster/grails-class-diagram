import org.codehaus.groovy.grails.commons.GrailsControllerClass


class ControllerDetail extends ClassDetail {

  GrailsControllerClass gcc

  String getNodeName() {
    "${gcc}Controller" - "Artefact > "
  }


  List getInterestingAssociations() {
    def injectedServices = gcc.properties['propertyDescriptors'].findAll { it.name =~ 'Service' }*.name
    injectedServices?.each { s ->
        dotBuilder.from(diagramClass.name+"Controller").to(
            ClassDiagramUtil.initCap(s),
            [arrowhead:cfg.arrows.references, arrowtail:cfg.arrows.none, dir:'both'])
    }
  }

  List getSubClasses() {

  }

  List getInterestingProperties() {

  }

  List getInterestingMethods() {

  }

}