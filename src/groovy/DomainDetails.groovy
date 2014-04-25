
class DomainDetails extends ClassDetails {

    // TO deal with the issue to know the Grails 's injected methods,
    // we list here the commonsMethod between Grails domain class, supposed to be injected.
    // commonsMethods is populated in buildDomainClasses()
    List commonsMethods = new ArrayList()

    List<EmbeddedClassDetail> embeddedClasses
    List<EnumDetail> enumClasses


    DomainDetails(grailsApplication, ClassDiagramPreferences prefs) {
        super(grailsApplication, prefs)

        def allDomains = grailsApplication.domainClasses

        classesToExclude = excludeSelection(allDomains)
        classesToDiagram = includeSelection(allDomains) - classesToExclude

        def props = classesToDiagram*.properties.flatten()
        embeddedClasses = props.findAll { it.embedded }.type.unique()
        enumClasses = props.findAll { it.enum }.type.unique()

        determineCommonMethods()
    }


    Set getAllPackageNames() {
        def packages = super.getAllPackageNames()
        if (! prefs.showEmbeddedAsProperty)
            packages += super.getPackageNames(embeddedClasses)
        if (! prefs.showEnumAsProperty)
            packages += super.getPackageNames(enumClasses)
    }


    Set getClassesToDiagramForPackage(String packageName) {
        Set cls = super.getClassesToDiagramForPackage(packageName)
        // TODO: this should be able to be cleaned up further
        // need to figure out why getPackageName() is doing what it's doing
        cls << embeddedClasses.findAll { ClassDiagramUtil.getPackageName(it) == packageName }
        cls << enumClasses.findAll { ClassDiagramUtil.getPackageName(it) == packageName }
        cls
    }


    private determineCommonMethods() {
        // TO deal with the issue to know the Grails 's injected methods,
        // we list commonsMethod between Grails domain class, supposed to be injected.

        // We start to list all methods from the first domain class, and then remove
        // all distinct methods from other domains. At the end, we suppose to have injected,
        // or at last common to all domain
        if(prefs?.showMethods) {
            List tmpCommonsMethods = new ArrayList()
            classesToDiagram.each { domainClass ->
                if (domainClass == domainClasses.first()) {
                    // First element
                    domainClass.clazz.declaredMethods.findAll().each {
                        commonsMethods.push(it.name)

                    }
                 }
                 else {
                      tmpCommonsMethods = new ArrayList()
                      domainClass.clazz.declaredMethods.findAll().each {
                          if(commonsMethods.contains(it.name)) {
                              tmpCommonsMethods.push(it.name)
                          }
                      }
                      commonsMethods = tmpCommonsMethods
                 }
            }
        }
    }

}