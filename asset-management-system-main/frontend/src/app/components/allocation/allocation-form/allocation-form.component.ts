import { Component, EventEmitter, OnInit, Output, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil, of, finalize } from 'rxjs';
import { AssetService } from '../../../services/asset.service';
import { UserService } from '../../../services/user.service';
import { Asset, User } from '../../../models/models';

@Component({
  selector: 'app-allocation-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './allocation-form.component.html',
  styleUrls: ['./allocation-form.component.css']
})
export class AllocationFormComponent implements OnInit, OnDestroy {
  @Input() asset: Asset | null = null;
  @Output() save = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();

  allocationForm: FormGroup;
  
  // Asset Autocomplete
  assetSearch$ = new Subject<string>();
  assetSuggestions: Asset[] = [];
  selectedAsset: Asset | null = null;
  loadingAssets = false;
  showAssetDropdown = false;

  // User Autocomplete
  userSearch$ = new Subject<string>();
  userSuggestions: User[] = [];
  selectedUser: User | null = null;
  loadingUsers = false;
  showUserDropdown = false;
  minDate: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private assetService: AssetService,
    private userService: UserService
  ) {
    this.allocationForm = this.fb.group({
      assetId: [null, Validators.required],
      userId: [null, Validators.required],
      allocationDate: [new Date().toISOString().split('T')[0], Validators.required],
      expectedReturnDate: [''],
      notes: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    if (this.asset) {
      this.selectAsset(this.asset);
    }
    
    // Setup Asset search
    this.assetSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) return of({ content: [] });
        this.loadingAssets = true;
        return this.assetService.getAssets({ status: 'AVAILABLE', search: query }).pipe(
          finalize(() => this.loadingAssets = false)
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe(res => {
      this.assetSuggestions = res.content;
      this.showAssetDropdown = this.assetSuggestions.length > 0;
    });

    // Setup User search
    this.userSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) return of([]);
        this.loadingUsers = true;
        return this.userService.searchUsers(query).pipe(
          finalize(() => this.loadingUsers = false)
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe(res => {
      this.userSuggestions = res;
      this.showUserDropdown = this.userSuggestions.length > 0;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onAssetInput(event: any): void {
    const val = event.target.value;
    if (!val) {
      this.selectedAsset = null;
      this.allocationForm.get('assetId')?.setValue(null);
    }
    this.assetSearch$.next(val);
  }

  selectAsset(asset: Asset): void {
    this.selectedAsset = asset;
    this.allocationForm.get('assetId')?.setValue(asset.id);
    this.minDate = asset.purchaseDate ? asset.purchaseDate.split('T')[0] : null;
    
    // If current allocation date is before new min date, reset it
    const currentAllocDate = this.allocationForm.get('allocationDate')?.value;
    if (this.minDate && currentAllocDate && currentAllocDate < this.minDate) {
      this.allocationForm.patchValue({ allocationDate: this.minDate });
    }
    
    this.showAssetDropdown = false;
  }

  onUserInput(event: any): void {
    const val = event.target.value;
    if (!val) {
      this.selectedUser = null;
      this.allocationForm.get('userId')?.setValue(null);
    }
    this.userSearch$.next(val);
  }

  selectUser(user: User): void {
    this.selectedUser = user;
    this.allocationForm.get('userId')?.setValue(user.id);
    this.showUserDropdown = false;
  }

  submit(): void {
    if (this.allocationForm.invalid) {
      this.allocationForm.markAllAsTouched();
      return;
    }
    this.save.emit(this.allocationForm.value);
  }
}
