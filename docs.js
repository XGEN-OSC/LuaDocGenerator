let currentDoc = null;
let currentFilter = '';
let classRegistry = new Map(); // Map of className -> {class, namespace}

// File input handler
document.getElementById('jsonFile').addEventListener('change', function(e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(event) {
            try {
                currentDoc = JSON.parse(event.target.result);
                buildClassRegistry(currentDoc);
                renderNavigation(currentDoc);
                showWelcomeMessage();
            } catch (error) {
                alert('Error parsing JSON file: ' + error.message);
            }
        };
        reader.readAsText(file);
    }
});

// Search functionality
document.getElementById('searchInput').addEventListener('input', function(e) {
    currentFilter = e.target.value.toLowerCase();
    if (currentDoc) {
        renderNavigation(currentDoc);
    }
});

// Build a registry of all classes for type linking
function buildClassRegistry(doc) {
    classRegistry.clear();
    if (!doc.namespaces) return;
    
    doc.namespaces.forEach(namespace => {
        if (namespace.classes) {
            namespace.classes.forEach(clazz => {
                classRegistry.set(clazz.name, {
                    class: clazz,
                    namespace: namespace.name
                });
            });
        }
    });
    
}

// Render navigation sidebar
function renderNavigation(doc) {
    const nav = document.getElementById('navigation');
    nav.innerHTML = '';

    if (!doc.namespaces || doc.namespaces.length === 0) {
        nav.innerHTML = '<p class="placeholder">No namespaces found</p>';
        return;
    }

    doc.namespaces.forEach(namespace => {
        const section = document.createElement('div');
        section.className = 'nav-section';

        const title = document.createElement('h3');
        title.textContent = namespace.name;
        section.appendChild(title);

        // Classes
        if (namespace.classes && namespace.classes.length > 0) {
            const filteredClasses = filterItems(namespace.classes, 'name');
            if (filteredClasses.length > 0) {
                appendNavItems(section, 'Classes', filteredClasses, 'class', namespace.name);
            }
        }

        // Global Functions - grouped together
        if (namespace.functions && namespace.functions.length > 0) {
            const filteredFunctions = filterItems(namespace.functions, 'name');
            if (filteredFunctions.length > 0) {
                appendGlobalsNavItem(section, 'Globals', filteredFunctions, namespace);
            }
        }

        // Fields
        if (namespace.fields && namespace.fields.length > 0) {
            const filteredFields = filterItems(namespace.fields, 'name');
            if (filteredFields.length > 0) {
                appendNavItems(section, 'Fields', filteredFields, 'field', namespace.name);
            }
        }

        nav.appendChild(section);
    });
}

function filterItems(items, nameProperty) {
    if (!currentFilter) return items;
    return items.filter(item => 
        item[nameProperty].toLowerCase().includes(currentFilter)
    );
}

function appendNavItems(section, title, items, type, namespaceName) {
    const subtitle = document.createElement('h4');
    subtitle.textContent = title;
    subtitle.style.fontSize = '0.85rem';
    subtitle.style.marginTop = '0.75rem';
    subtitle.style.marginBottom = '0.25rem';
    subtitle.style.color = '#7f8c8d';
    section.appendChild(subtitle);

    const list = document.createElement('ul');
    list.className = 'nav-list';

    items.forEach(item => {
        const li = document.createElement('li');
        li.className = `nav-item nav-item-${type}`;
        li.textContent = item.name;
        li.addEventListener('click', () => {
            document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
            li.classList.add('active');
            renderItem(item, type, namespaceName);
        });
        list.appendChild(li);
    });

    section.appendChild(list);
}

function appendGlobalsNavItem(section, title, functions, namespace) {
    const subtitle = document.createElement('h4');
    subtitle.textContent = title;
    subtitle.style.fontSize = '0.85rem';
    subtitle.style.marginTop = '0.75rem';
    subtitle.style.marginBottom = '0.25rem';
    subtitle.style.color = '#7f8c8d';
    section.appendChild(subtitle);

    const list = document.createElement('ul');
    list.className = 'nav-list';

    const li = document.createElement('li');
    li.className = 'nav-item nav-item-function';
    li.textContent = 'Global Functions';
    li.addEventListener('click', () => {
        document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
        li.classList.add('active');
        renderGlobals(functions, namespace.name);
    });
    list.appendChild(li);

    section.appendChild(list);
}

function showWelcomeMessage() {
    const content = document.getElementById('docContent');
    content.innerHTML = `
        <div class="placeholder-content">
            <h2>Documentation Loaded</h2>
            <p>Select an item from the navigation to view its documentation.</p>
        </div>
    `;
}

function renderItem(item, type, namespaceName) {
    const content = document.getElementById('docContent');
    content.innerHTML = '';

    const section = document.createElement('div');
    section.className = 'doc-section';

    // Header
    const header = document.createElement('div');
    header.className = 'doc-header';

    const title = document.createElement('h1');
    title.className = 'doc-title';
    title.textContent = item.name;
    header.appendChild(title);

    const typeLabel = document.createElement('span');
    typeLabel.className = 'doc-type';
    typeLabel.textContent = type;
    header.appendChild(typeLabel);

    const nsLabel = document.createElement('span');
    nsLabel.className = 'doc-type';
    nsLabel.style.backgroundColor = '#95a5a6';
    nsLabel.style.marginLeft = '0.5rem';
    nsLabel.textContent = namespaceName;
    header.appendChild(nsLabel);

    section.appendChild(header);

    // Description
    if (item.description) {
        const desc = document.createElement('div');
        desc.className = 'description';
        desc.textContent = item.description;
        section.appendChild(desc);
    }

    // Type-specific rendering
    if (type === 'class') {
        renderClass(section, item);
    } else if (type === 'function') {
        renderFunction(section, item);
    } else if (type === 'field') {
        renderField(section, item);
    }

    content.appendChild(section);
}

function renderGlobals(functions, namespaceName) {
    const content = document.getElementById('docContent');
    content.innerHTML = '';

    const section = document.createElement('div');
    section.className = 'doc-section';

    // Header
    const header = document.createElement('div');
    header.className = 'doc-header';

    const title = document.createElement('h1');
    title.className = 'doc-title';
    title.textContent = 'Global Functions';
    header.appendChild(title);

    const typeLabel = document.createElement('span');
    typeLabel.className = 'doc-type';
    typeLabel.textContent = 'globals';
    header.appendChild(typeLabel);

    const nsLabel = document.createElement('span');
    nsLabel.className = 'doc-type';
    nsLabel.style.backgroundColor = '#95a5a6';
    nsLabel.style.marginLeft = '0.5rem';
    nsLabel.textContent = namespaceName;
    header.appendChild(nsLabel);

    section.appendChild(header);

    // Description
    const desc = document.createElement('div');
    desc.className = 'description';
    desc.textContent = `Global functions in the ${namespaceName} namespace.`;
    section.appendChild(desc);

    // Separate static and non-static functions
    const staticFunctions = functions.filter(f => f.isStatic);
    const instanceFunctions = functions.filter(f => !f.isStatic);
    
    // Render static functions
    if (staticFunctions.length > 0) {
        const subsection = document.createElement('div');
        subsection.className = 'subsection';

        const subsectionTitle = document.createElement('h2');
        subsectionTitle.className = 'subsection-title';
        subsectionTitle.textContent = 'Static Functions';
        subsection.appendChild(subsectionTitle);

        const list = document.createElement('ul');
        list.className = 'item-list';

        staticFunctions.forEach(func => {
            const li = document.createElement('li');
            li.className = 'item';
            renderFunctionContent(li, func);
            list.appendChild(li);
        });

        subsection.appendChild(list);
        section.appendChild(subsection);
    }

    // Render instance functions
    if (instanceFunctions.length > 0) {
        const subsection = document.createElement('div');
        subsection.className = 'subsection';

        const subsectionTitle = document.createElement('h2');
        subsectionTitle.className = 'subsection-title';
        subsectionTitle.textContent = 'Instance Functions';
        subsection.appendChild(subsectionTitle);

        const list = document.createElement('ul');
        list.className = 'item-list';

        instanceFunctions.forEach(func => {
            const li = document.createElement('li');
            li.className = 'item';
            renderFunctionContent(li, func);
            list.appendChild(li);
        });

        subsection.appendChild(list);
        section.appendChild(subsection);
    }

    content.appendChild(section);
}

function renderClass(section, classItem) {
    // Fields
    if (classItem.fields && classItem.fields.length > 0) {
        const subsection = document.createElement('div');
        subsection.className = 'subsection';

        const title = document.createElement('h2');
        title.className = 'subsection-title';
        title.textContent = 'Fields';
        subsection.appendChild(title);

        const list = document.createElement('ul');
        list.className = 'item-list';

        classItem.fields.forEach(field => {
            const li = document.createElement('li');
            li.className = 'item';

            const itemHeader = document.createElement('div');
            itemHeader.className = 'item-header';

            const name = document.createElement('span');
            name.className = 'item-name';
            name.textContent = field.name;
            itemHeader.appendChild(name);

            const type = createTypeElement(field.type);
            itemHeader.appendChild(type);

            if (field.isStatic) {
                const badge = document.createElement('span');
                badge.className = 'item-badge static';
                badge.textContent = 'static';
                itemHeader.appendChild(badge);
            }

            li.appendChild(itemHeader);

            if (field.description) {
                const desc = document.createElement('div');
                desc.className = 'item-description';
                desc.textContent = field.description;
                li.appendChild(desc);
            }

            list.appendChild(li);
        });

        subsection.appendChild(list);
        section.appendChild(subsection);
    }

    // Functions
    if (classItem.functions && classItem.functions.length > 0) {
        // Separate static and instance methods
        const staticFunctions = classItem.functions.filter(f => f.isStatic);
        const instanceFunctions = classItem.functions.filter(f => !f.isStatic);
        
        // Render static methods first
        if (staticFunctions.length > 0) {
            const subsection = document.createElement('div');
            subsection.className = 'subsection';

            const title = document.createElement('h2');
            title.className = 'subsection-title';
            title.textContent = 'Static Methods';
            subsection.appendChild(title);

            const list = document.createElement('ul');
            list.className = 'item-list';

            staticFunctions.forEach(func => {
                const li = document.createElement('li');
                li.className = 'item';
                renderFunctionContent(li, func);
                list.appendChild(li);
            });

            subsection.appendChild(list);
            section.appendChild(subsection);
        }
        
        // Render instance methods
        if (instanceFunctions.length > 0) {
            const subsection = document.createElement('div');
            subsection.className = 'subsection';

            const title = document.createElement('h2');
            title.className = 'subsection-title';
            title.textContent = 'Instance Methods';
            subsection.appendChild(title);

            const list = document.createElement('ul');
            list.className = 'item-list';

            instanceFunctions.forEach(func => {
                const li = document.createElement('li');
                li.className = 'item';
                renderFunctionContent(li, func);
                list.appendChild(li);
            });

            subsection.appendChild(list);
            section.appendChild(subsection);
        }
    }
}

function renderFunction(section, func) {
    const container = document.createElement('div');
    renderFunctionContent(container, func);
    section.appendChild(container);
}

function renderFunctionContent(container, func) {
    // Function header
    const itemHeader = document.createElement('div');
    itemHeader.className = 'item-header';

    const name = document.createElement('span');
    name.className = 'item-name';
    name.textContent = func.name;
    itemHeader.appendChild(name);

    if (func.isStatic) {
        const badge = document.createElement('span');
        badge.className = 'item-badge static';
        badge.textContent = 'static';
        itemHeader.appendChild(badge);
    }

    container.appendChild(itemHeader);

    // Function signature
    const signature = document.createElement('div');
    signature.className = 'function-signature';
    const params = func.parameters.map(p => 
        `${p.name}: ${p.type}${p.optional ? '?' : ''}`
    ).join(', ');
    const returns = func.returns.map(r => r.type).join(', ') || 'void';
    signature.textContent = `function ${func.name}(${params}): ${returns}`;
    container.appendChild(signature);

    if (func.description) {
        const desc = document.createElement('div');
        desc.className = 'item-description';
        desc.textContent = func.description;
        container.appendChild(desc);
    }

    // Parameters
    if (func.parameters && func.parameters.length > 0) {
        const paramsDiv = document.createElement('div');
        paramsDiv.className = 'parameters';

        const paramTitle = document.createElement('div');
        paramTitle.className = 'param-title';
        paramTitle.textContent = 'Parameters:';
        paramsDiv.appendChild(paramTitle);

        const paramList = document.createElement('ul');
        paramList.className = 'param-list';

        func.parameters.forEach(param => {
            const li = document.createElement('li');
            li.className = 'param-item';

            const paramHeader = document.createElement('div');
            paramHeader.style.marginBottom = '0.25rem';

            const paramName = document.createElement('span');
            paramName.className = 'item-name';
            paramName.style.fontSize = '0.95rem';
            paramName.textContent = param.name;
            paramHeader.appendChild(paramName);

            const paramType = createTypeElement(param.type + (param.optional ? '?' : ''));
            paramType.style.fontSize = '0.95rem';
            paramHeader.appendChild(paramType);

            if (param.optional) {
                const badge = document.createElement('span');
                badge.className = 'item-badge optional';
                badge.textContent = 'optional';
                badge.style.marginLeft = '0.5rem';
                paramHeader.appendChild(badge);
            }

            li.appendChild(paramHeader);

            if (param.description) {
                const desc = document.createElement('div');
                desc.className = 'item-description';
                desc.style.fontSize = '0.85rem';
                desc.textContent = param.description;
                li.appendChild(desc);
            }

            paramList.appendChild(li);
        });

        paramsDiv.appendChild(paramList);
        container.appendChild(paramsDiv);
    }

    // Returns
    if (func.returns && func.returns.length > 0) {
        const returnsDiv = document.createElement('div');
        returnsDiv.className = 'returns';

        const returnTitle = document.createElement('div');
        returnTitle.className = 'return-title';
        returnTitle.textContent = 'Returns:';
        returnsDiv.appendChild(returnTitle);

        const returnList = document.createElement('ul');
        returnList.className = 'return-list';

        func.returns.forEach(ret => {
            const li = document.createElement('li');
            li.className = 'return-item';

            const retType = createTypeElement(ret.type);
            // Remove the ': ' prefix by removing the first text node
            if (retType.firstChild && retType.firstChild.nodeType === Node.TEXT_NODE) {
                retType.firstChild.remove();
            }
            li.appendChild(retType);

            if (ret.description) {
                const desc = document.createElement('div');
                desc.className = 'item-description';
                desc.style.fontSize = '0.85rem';
                desc.textContent = ret.description;
                li.appendChild(desc);
            }

            returnList.appendChild(li);
        });

        returnsDiv.appendChild(returnList);
        container.appendChild(returnsDiv);
    }
}

function renderField(section, field) {
    const container = document.createElement('div');

    const itemHeader = document.createElement('div');
    itemHeader.className = 'item-header';

    const name = document.createElement('span');
    name.className = 'item-name';
    name.textContent = field.name;
    itemHeader.appendChild(name);

    const type = createTypeElement(field.type);
    itemHeader.appendChild(type);

    if (field.isStatic) {
        const badge = document.createElement('span');
        badge.className = 'item-badge static';
        badge.textContent = 'static';
        itemHeader.appendChild(badge);
    }

    container.appendChild(itemHeader);

    if (field.description) {
        const desc = document.createElement('div');
        desc.className = 'item-description';
        desc.textContent = field.description;
        container.appendChild(desc);
    }

    section.appendChild(container);
}

// Helper function to create a clickable type element
function createTypeElement(typeString) {
    const container = document.createElement('span');
    container.className = 'item-type';
    container.textContent = ': ';
    
    // Parse the type string to handle union types and optional markers
    const types = parseTypeString(typeString);
    
    types.forEach((typePart, index) => {
        if (index > 0) {
            container.appendChild(document.createTextNode(typePart.separator));
        }
        
        const typeSpan = document.createElement('span');
        typeSpan.textContent = typePart.name;
        
        // Don't make separators and punctuation clickable
        const trimmedName = typePart.name.trim();
        if (trimmedName !== '|' && trimmedName !== '&' && trimmedName !== '<' && trimmedName !== '>' && trimmedName !== ',') {
            // Check if this type is a registered class
            const cleanType = trimmedName.replace(/\?$/, ''); // Remove optional marker at end
            if (classRegistry.has(cleanType)) {
                typeSpan.classList.add('clickable-type');
                typeSpan.dataset.classType = cleanType;
                typeSpan.style.cssText = 'cursor: pointer; text-decoration: underline dotted; color: var(--accent-color) !important;';
                typeSpan.title = 'Click to view ' + cleanType;

                typeSpan.addEventListener('click', (e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    const classInfo = classRegistry.get(cleanType);
                    if (classInfo) {
                        // Clear active navigation
                        document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
                        renderItem(classInfo.class, 'class', classInfo.namespace);
                    }
                });
            }
        }
        
        container.appendChild(typeSpan);
    });
    
    return container;
}

// Parse type string into parts (handling union types like "string|number")
function parseTypeString(typeString) {
    const parts = [];
    // Split by | and & but also handle generic types like table<string, XCore.Player>
    const tokens = typeString.split(/(\||&|<|>|,)/);
    
    for (let i = 0; i < tokens.length; i++) {
        const token = tokens[i].trim();
        if (!token) continue;
        
        if (token === '|' || token === '&') {
            parts.push({ name: ' ' + token + ' ', separator: '' });
        } else if (token === '<' || token === '>' || token === ',') {
            parts.push({ name: token, separator: '' });
        } else {
            parts.push({ name: token, separator: '' });
        }
    }
    
    return parts;
}

// Theme toggle: persist preference in localStorage and apply .dark on root
function applyTheme(theme) {
    const root = document.documentElement;
    if (theme === 'dark') {
        root.classList.add('dark');
        const btn = document.getElementById('themeToggle');
        if (btn) btn.textContent = '☀️';
    } else {
        root.classList.remove('dark');
        const btn = document.getElementById('themeToggle');
        if (btn) btn.textContent = '🌙';
    }
    try { localStorage.setItem('theme', theme); } catch (e) { /* ignore */ }
}

function initThemeToggle() {
    const btn = document.getElementById('themeToggle');
    if (!btn) return;

    // Determine initial theme: stored preference, then system preference, then default light
    let theme = null;
    try { theme = localStorage.getItem('theme'); } catch (e) { theme = null; }

    if (!theme) {
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        theme = prefersDark ? 'dark' : 'light';
    }

    applyTheme(theme);

    btn.addEventListener('click', () => {
        const current = document.documentElement.classList.contains('dark') ? 'dark' : 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        applyTheme(next);
    });
}

// Initialize theme when DOM content is ready
document.addEventListener('DOMContentLoaded', initThemeToggle);
