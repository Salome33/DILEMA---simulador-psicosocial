import { Component, ElementRef, effect, input, model, viewChild } from '@angular/core';

@Component({
  selector: 'app-editor',
  template: `
    <div class="editor">
      <div class="editor-bar">
        <button type="button" (mousedown)="$event.preventDefault()" (click)="cmd('bold')" [disabled]="bloqueado()">Negrita</button>
        <button type="button" (mousedown)="$event.preventDefault()" (click)="cmd('italic')" [disabled]="bloqueado()">Cursiva</button>
        <button type="button" (mousedown)="$event.preventDefault()" (click)="cmd('insertUnorderedList')" [disabled]="bloqueado()">Lista</button>
      </div>
      <div #area class="editor-area" [attr.contenteditable]="bloqueado() ? 'false' : 'true'" (input)="emitir()"></div>
    </div>
  `
})
export class EditorTextoComponent {
  valor = model('');
  bloqueado = input(false);
  private area = viewChild<ElementRef<HTMLDivElement>>('area');

  constructor() {
    effect(() => {
      const texto = this.valor() || '';
      const nodo = this.area()?.nativeElement;
      if (!nodo || document.activeElement === nodo) return;
      if (nodo.innerHTML !== texto) nodo.innerHTML = texto;
    });
  }

  emitir() {
    this.valor.set(this.area()?.nativeElement.innerHTML ?? '');
  }

  cmd(comando: string) {
    document.execCommand(comando);
    this.emitir();
  }
}
