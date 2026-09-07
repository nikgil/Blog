// AI generated
const COLLAPSED_CODE_LINES = 10;

document.querySelectorAll(".post__body pre > code").forEach((code, index) => {
    const lines = code.textContent.replace(/\r?\n$/, "").split(/\r?\n/);
    if (lines.length <= COLLAPSED_CODE_LINES) {
        return;
    }

    const pre = code.parentElement;
    const wrapper = document.createElement("div");
    const toggle = document.createElement("button");
    const codeId = `post-code-${index + 1}`;
    const codeStyles = getComputedStyle(code);
    const computedLineHeight = Number.parseFloat(codeStyles.lineHeight);
    const lineHeight = Number.isNaN(computedLineHeight)
        ? Number.parseFloat(codeStyles.fontSize) * 1.5
        : computedLineHeight;

    wrapper.className = "post-code";
    code.classList.add("post-code__source");
    code.id = codeId;
    code.style.setProperty(
        "--post-code-collapsed-height",
        `${lineHeight * COLLAPSED_CODE_LINES}px`
    );

    toggle.className = "post-code__toggle";
    toggle.type = "button";
    toggle.setAttribute("aria-controls", codeId);
    toggle.setAttribute("aria-expanded", "false");
    toggle.textContent = `Show all ${lines.length} lines`;

    pre.before(wrapper);
    wrapper.append(pre, toggle);

    toggle.addEventListener("click", () => {
        const expanded = wrapper.classList.toggle("post-code--expanded");

        toggle.setAttribute("aria-expanded", String(expanded));
        toggle.textContent = expanded
            ? "Show first 10 lines"
            : `Show all ${lines.length} lines`;
    });
});
