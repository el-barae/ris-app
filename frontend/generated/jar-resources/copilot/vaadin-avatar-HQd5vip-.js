import { L as a, H as r, O as t, K as e } from "./copilot-tF9CaZ8e.js";
const i = {
  tagName: "vaadin-avatar",
  displayName: "Avatar",
  elements: [
    {
      selector: "vaadin-avatar",
      displayName: "Root element",
      properties: [
        a.backgroundColor,
        a.borderColor,
        a.borderWidth,
        {
          propertyName: "--vaadin-avatar-size",
          displayName: "Size",
          editorType: r.range,
          presets: t.lumoSize,
          icon: "square"
        }
      ]
    },
    {
      selector: "vaadin-avatar::part(abbr)",
      displayName: "Abbreviation",
      properties: [e.textColor, e.fontWeight]
    }
  ]
};
export {
  i as default
};
