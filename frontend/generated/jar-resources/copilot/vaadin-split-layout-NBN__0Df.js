import { L as a } from "./copilot-tF9CaZ8e.js";
const e = {
  tagName: "vaadin-split-layout",
  displayName: "Split Layout",
  elements: [
    {
      selector: "vaadin-split-layout",
      displayName: "Layout",
      properties: [
        a.backgroundColor,
        a.borderColor,
        a.borderWidth,
        a.borderRadius
      ]
    },
    {
      selector: "vaadin-split-layout::part(splitter)",
      displayName: "Splitter bar",
      properties: [a.backgroundColor]
    },
    {
      selector: "vaadin-split-layout::part(handle)::after",
      displayName: "Splitter handle",
      properties: [a.backgroundColor]
    }
  ]
};
export {
  e as default
};
