(function() {
    function initializeTooltip() {
        if (!document.body) return setTimeout(initializeTooltip, 50);
        var tooltip = document.createElement('div');
        tooltip.style.position = 'fixed';
        tooltip.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
        tooltip.style.color = 'white';
        tooltip.style.padding = '5px';
        tooltip.style.borderRadius = '3px';
        tooltip.style.fontSize = '12px';
        tooltip.style.zIndex = '9999';
        tooltip.style.pointerEvents = 'none';
        document.body.appendChild(tooltip);

        function getSelector(element) {
            if (!element) return null;

            if (element.matches("button, a, [onclick], [data-action]")) {
                return getElementInfo(element);
            }

            var clickableParent = element.closest("button, a, [onclick], [data-action]");
            return clickableParent ? getElementInfo(clickableParent) : getElementInfo(element);
        }

        function getElementInfo(element) {
            if (!element) return "Unbekanntes Element";
            if (element.id) return "#" + element.id;
            if (element.className) return "." + element.className.split(" ").join(".");
            return element.tagName.toLowerCase();
        }

        document.addEventListener('mouseover', function(event) {
            var el = event.target;
            var selector = getSelector(el);
            tooltip.textContent = selector;
            tooltip.style.top = (event.clientY + 10) + 'px';
            tooltip.style.left = (event.clientX + 10) + 'px';
            tooltip.style.display = 'block';
        });

        document.addEventListener('mouseout', function() {
            tooltip.style.display = 'none';
        });
    }
    document.addEventListener('DOMContentLoaded', initializeTooltip);
})
