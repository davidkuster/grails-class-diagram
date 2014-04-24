
class DomainDetails extends ClassDetails {

    def embeddedClasses
    def enumClasses


    DomainDetails(grailsApplication, ClassDiagramPreferences prefs) {
        super(prefs)

        def allDomains = grailsApplication.domainClasses

        classesToExclude = excludeSelection(allDomains)
        classesToDiagram = includeSelection(allDomains) - classesToExclude

        def props = classesToDiagram*.properties.flatten()
        embeddedClasses = props.findAll { it.embedded }.type.unique()
        enumClasses = props.findAll { it.enum }.type.unique()
    }


    Set getAllPackageNames() {
        def packages = super.getAllPackageNames()
        if (! prefs.showEmbeddedAsProperty)
            packages += super.getPackageNames(embeddedClasses)
        if (! prefs.showEnumAsProperty)
            packages += super.getPackageNames(enumClasses)
    }


}