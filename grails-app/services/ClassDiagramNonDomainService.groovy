import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import java.util.regex.Pattern
import grails.util.Holders

/**
 * Service that takes classes other than Grails domains (controllers/services probably)
 * and turns them into a domain class diagram.
 */
class ClassDiagramNonDomainService extends ClassDiagramService {

    static transactional = false

    def grailsApplication
    def config = Holders.config


    byte[] createDiagram(prefs) {
        def controllerServiceDetails = new ControllerServiceDetails(grailsApplication, prefs)
        def dotBuilder = createDotDiagram(controllerServiceDetails, prefs)
        dotBuilder.createDiagram(prefs.outputFormat ?: "png")
    }


    private DotBuilder createDotDiagram(classDetails, ClassDiagramPreferences prefs) {
        def skin = config.classDiagram.skins?."${prefs.skin}"

        def dotBuilder = new DotBuilder()
        dotBuilder.digraph {
            buildGraphDefaults(dotBuilder, skin, prefs)
            rankdir ("${prefs.graphOrientation}")

            if (! prefs.showPackages) {
                // build all classes
                buildAllClasses(dotBuilder, classDetails, prefs)
            }
            else {
                // build classes per package
                classDetails.getAllPackageNames().each { packageName ->
                    subgraph("cluster_"+packageName) {
                        skin.packageStyle.each {
                            "${it.key}" ("${it.value}")
                        }
                        fontsize("${prefs.fontsize}")
                        labeljust("l")
                        label ("${packageName ?: '<root>'}")

                        buildClassPerPackage(dotBuilder, classDetails, packageName, prefs)
                    }
                }
            }

            buildRelations(dotBuilder, classDetails, prefs)
        }

        dotBuilder
    }


    private void buildAllClasses(dotBuilder, classDetails, prefs) {
        if (classDetails instanceof DomainDetails) {
            // build domain diagram
            buildDomainClasses(dotBuilder, classDetails.classesToDiagram, prefs)
            buildGenericClasses(dotBuilder, classDetails.embeddedClasses, 'showEmbeddedAsProperty', prefs)
            buildGenericClasses(dotBuilder, classDetails.enumClasses, 'showEnumAsProperty', prefs)
        }
        else if (classDetails instanceof ControllerServiceDetails) {
            // build controller/service diagram
            buildControllerServiceClasses(dotBuilder, classDetails.classesToDiagram, prefs)
        }
    }

    private void buildClassPerPackage(dotBuilder, classDetails, packageName, prefs) {
        if (classDetails instanceof DomainDetails) {
            // build domain diagram (per package)
            buildDomainClasses(dotBuilder, classDetails.classesToDiagram.findAll { ClassDiagramUtil.getPackageName(it) == packageName }, prefs)
            buildGenericClasses(dotBuilder, classDetails.embeddedClasses.findAll { ClassDiagramUtil.getPackageName(it) == packageName }, 'showEmbeddedAsProperty', prefs)
            buildGenericClasses(dotBuilder, classDetails.enumClasses.findAll { ClassDiagramUtil.getPackageName(it) == packageName}, 'showEnumAsProperty', prefs)
        }
        else {
            // build controller/service diagram (per package)
            buildControllerServiceClasses(dotBuilder, classDetails.classesToDiagram.findAll { ClassDiagramUtil.getPackageName(it) == packageName }, prefs)
        }
    }


    private void buildGenericClasses(dotBuilder, classes, showProp, prefs) {
        if (! prefs."$showProp") {
            classes.each { clazz ->
                dotBuilder."${clazz.simpleName}" ([label:formatNodeLabel(clazz, prefs)])
            }
        }
    }

    private void buildControllerServiceClasses(dotBuilder, classes, prefs) {
        println "building controller service classes: $classes"
        classes.each { clazz ->
            def name
            if (clazz instanceof GrailsControllerClass) name = "${clazz}Controller" - "Artefact > "
            else if (clazz instanceof GrailsServiceClass) name = "${clazz}Service" - "Artefact > "
            dotBuilder."$name" ([label:formatNodeLabel(clazz, prefs)])
        }
    }

    private void buildRelations(dotBuilder, classDetails, prefs) {
        def cfg = config.classDiagram.associations
        def classesToDiagram = classDetails.classesToDiagram
        def classesToExclude = classDetails.classesToExclude

        println "building relations on classes $classesToDiagram"
        println "excluding $classesToExclude"

        if (classDetails instanceof DomainDetails) {
            classesToDiagram.each { diagramClass ->
                // build associations
                getInterestingAssociations(diagramClass, classesToExclude, prefs).each { ass ->
                    dotBuilder.from(diagramClass.name).to(
                        ass.referencedDomainClass?.name ?: ass.type.simpleName,
                        getAssociationProps(ass, prefs))
                }
                // build inheritance
                diagramClass.subClasses.each { subClass ->
                    if (subClass.clazz.superclass == diagramClass.clazz) { // GRAILSPLUGINS-1740: domainClass.subClasses also returns all sub-sub-classes!
                        dotBuilder.from(diagramClass.name).to(
                            subClass.name,
                            [arrowhead:cfg.arrows.none, arrowtail:cfg.arrows.inherits, dir:'both'])
                    }
                }
            }
        }
        else if (classDetails instanceof ControllerServiceDetails) {
            classesToDiagram.each { diagramClass ->
                if (diagramClass instanceof GrailsControllerClass) {
                    // build associations
                    def injectedServices = diagramClass.properties['propertyDescriptors'].findAll { it.name =~ 'Service' }*.name
                    injectedServices?.each { s ->
                        dotBuilder.from(diagramClass.name+"Controller").to(
                            ClassDiagramUtil.initCap(s),
                            [arrowhead:cfg.arrows.references, arrowtail:cfg.arrows.none, dir:'both'])
                    }
                    // build inheritance
                }
                else if (diagramClass instanceof GrailsServiceClass) {
                    // build associations
                    def injectedServices = diagramClass.properties['propertyDescriptors'].findAll { it.name =~ 'Service' }*.name
                    injectedServices?.each { s ->
                        dotBuilder.from(diagramClass.name+"Service").to(
                            ClassDiagramUtil.initCap(s),
                            [arrowhead:cfg.arrows.references, arrowtail:cfg.arrows.none, dir:'both'])
                    }
                    // build inheritance
                }
            }
        }
    }


    /************************************************
    /* format methods
    /***********************************************/

    /**
     * @return Node label containing class name, properties, methods, and dividers
     */
    private String formatNodeLabel(cls, prefs) {
        def name
        if (cls instanceof GrailsDomainClass) name = cls.name
        else if (cls instanceof GrailsControllerClass) name = "${cls.name}Controller"
        else if (cls instanceof GrailsServiceClass) name = "${cls.name}Service"

        return (prefs.verticalOrientation ? "{" : "") + name +
                 formatProperties(cls, prefs) +
                 formatMethods(cls, prefs) +
                 (prefs.verticalOrientation ? "}" : "")
    }

    private String formatProperties(cls, prefs) {
        if (prefs?.showProperties) {
            def label = "|"
            if (cls instanceof GrailsDomainClass && cls.enum) {
                label += getInterestingProperties(cls, prefs).collect { ClassDiagramUtil.formatEnumProperty(it, prefs)}.join("\\l")
            } else {
                label += getInterestingProperties(cls, prefs).collect { ClassDiagramUtil.formatProperty(it, prefs)}.join("\\l")
            }
            label += "\\l" // get weird formatting without this one
            return label
        } else {
            ""
        }
    }

    private String formatMethods(cls, prefs) {
        if (prefs?.showMethods) {
            def label = "|"
            label += getInterestingMethods(cls, prefs).collect { ClassDiagramUtil.formatMethod(it, prefs)}.join("\\l")
            label += "\\l"
            return label
        } else {
            ""
        }
    }

    /**** end format methods ****/


    /************************************************
    /* get interesting data methods
    /***********************************************/

    private getInterestingProperties(cls, prefs) {
        if (cls instanceof GrailsDomainClass) {
            cls.properties.findAll { prop ->
                !(prop.name in ["id","version"]) &&
                (!prop.association || (prop.embedded && prefs.showEmbeddedAsProperty)) &&
                (!(prop.enum && !prefs.showEnumAsProperty)) &&
                (!prop.inherited)
            }
        }
        else if (cls instanceof GrailsControllerClass) {
            cls.clazz.declaredFields.findAll { field ->
                !field.name.startsWith("\$") &&
                !field.name.startsWith("__") &&
                !(field.name in ["metaClass","defaultAction","allowedMethods", "instanceControllerTagLibraryApi", "mimeTypesApi", "instanceControllersApi", "log", "instanceConvertersControllersApi"])
            }
        }
        else if (cls instanceof GrailsServiceClass) {
            cls.clazz.declaredFields.findAll { field ->
                !field.name.startsWith("\$") &&
                !field.name.startsWith("__") &&
                !(field.name in ["metaClass", "transactional", "log"])
            }
        }
        else if (cls.enum) {
            cls.declaredFields.findAll { field ->
                !field.name.startsWith("\$") &&
                !field.name.startsWith("__") &&
                !(field.name in ["metaClass", "MAX_VALUE", "MIN_VALUE"]) &&
                !field.name.startsWith("array\$\$")
            }
        }
        else { // Assume regular java class
            cls.declaredFields.findAll { field ->
                !field.name.startsWith("\$") &&
                !field.name.startsWith("__") &&
                !(field.name in ["metaClass"])
            }
        }
    }

    private getInterestingAssociations(GrailsDomainClass domainClass, excludeDomains, prefs) {
        domainClass.properties.findAll { prop ->
            (prop.association || prop.enum) && // All associations and enums
            !(prop.embedded && prefs.showEmbeddedAsProperty) && // except embedded if not configured so
            !(prop.enum && prefs.showEnumAsProperty) && // except enums if not configured so
            !prop.inherited && // except inherited stuff
            !(prop.bidirectional && domainClass.name > prop.referencedDomainClass.name) && // bidirectionals should only be mapped once
            !(prop.referencedDomainClass in excludeDomains)
        }
    }

    /**
     * Get methods declared in a class, filtering out all inherited and meta-added stuff.
     * Quite a few assumptions are made.
     * GrailsDomainClass or its super GrailsClass should have a getDeclaredMethods()!
     */
    private getInterestingMethods(cls, prefs) {
        if (cls instanceof GrailsDomainClass) {
            def methods = cls.clazz.declaredMethods
            def propertyNames = cls.properties*.name
            propertyNames += ["id","version", "hasMany", "belongsTo", "mappedBy", "mapping", "constraints", "embedded"]
            getDeclaredMethods(methods, propertyNames)
        }
        else if (cls instanceof GrailsControllerClass) {
            def methods = cls.clazz.declaredMethods
            def propertyNames = cls.properties.keySet()
            propertyNames += ["render", "bindData", "instanceControllerTagLibraryApi", "mimeTypesApi", "instanceControllersApi", "log", "instanceConvertersControllersApi"]
            getDeclaredMethods(methods, propertyNames)
        }
        else if (cls instanceof GrailsServiceClass)  {
            def methods = cls.clazz.declaredMethods
            def propertyNames = cls.properties.keySet()
            propertyNames += ["transactional", "log"]
            getDeclaredMethods(methods, propertyNames)
        }
        else if (cls.enum) {
            def methods = cls.declaredMethods
            methods -= methods.findAll { it.name in ["valueOf", "values", "next", "previous"]} // Removed even if overridden
            def propertyNames = cls.declaredFields*.name
            getDeclaredMethods(methods, propertyNames)
        }
        else { // Assume regular java class
            def methods = cls.declaredMethods
            def propertyNames = cls.declaredFields*.name
            getDeclaredMethods(methods, propertyNames)
        }
    }

    /**
     * Get methods declared in a class, filtering out all inherited and meta-added stuff.
     * Quite a few assumptions are made, no satisfactory solution found. Hack!
     * The Class.getDeclaredMethods() also includes decorated methods, which makes it essentially useless.
     * I think we need a grails getUndecoratedDeclaredMethods() that gives us what we really coded in the class, if that is possible.
     */
    private getDeclaredMethods(methods, propertyNames) {
        def filterMethods = methods.findAll { it.name =~ /\$/} // remove special methods containing $
        filterMethods += GroovyObject.methods.flatten() // remove metaClass, properties etc.
        filterMethods += Object.methods.flatten() // remove toString

        commonsMethods.each { methodtoremove ->
            filterMethods += methods.findAll {it.name =~ methodtoremove}
        }

        // filter out property-related methods
        methods.each { method ->
            ["get","is","set","addTo","removeFrom","render","bindData"].each { prefix ->
                propertyNames.each{ propertyName ->
                    if (method.name == prefix + ClassDiagramUtil.initCap(propertyName)) {
                        // TODO: Filter by signature, not just name
                        filterMethods += method
                    }
                }
            }
        }

        // Apply filter
        filterMethods.each {filterMethod ->
            methods -= methods.find { hasSameSignature(it, filterMethod)}
        }

        return methods
    }

    /**** end get interesting data methods ****/

}